package client;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import request.Request;

/**
 * Klasa zawiera implementacje wszystkich metod odpowiedzialnych za komunikacj� pomi�dzy klientem, a serwerem.
 * Implementuje ona interfejs Runnable, co oznacza �e klasa powinna zosta� wykorzystana jako w�tek.
 * Dzia�a na zasadzie wysy�ania Request�w do serwera.
 * Serwer odbiera obiekt typu "Request" i podejmuje okre�lone dzia�ania w zale�no�ci od zawarto�ci tego obiektu.
 * W�tek FileSynchronizer wykonuje tylko jedn� czynno�� w danym momencie,
 * np. prosi o list� plik�w danego u�ytkownika na serwerze. Nie zrobi niczego innego dop�ki nie otrzyma odpowiedzi
 * na ostatnio wys�any Request.
 * Mo�e pracowa� w dw�ch trybach:
 * -Pierwszy to synchronizacja plikow pomi�dzy klientem, a serwerem.
 * Oznacza to, �e w�tek FileSynchronizer najpierw poprosi serwer o list� plik�w znajduj�cych si� na serwerze,
 * nast�pnie por�wna j� z list� plik�w w folderze u�ytkownika. Je�eli na serwerze znajduj� si� pliki kt�rych
 * nie ma w folderze u�ytkownika to zostan� one pobrane. Z kolei kiedy na serwerze brakuje plik�w znajduj�cych
 * si� w folderze to zostan� one wys�ane. Na ko�cu zostanie jeszcze wys�any Request o przys�anie listy
 * u�ytkownik�w korzystaj�cych z serwera.
 * -Drugi to udost�pnienie pliku. FileSynchronizer wysy�a Request o udost�pnienie pliku nale��cego do nas,
 * innemu u�ytkonikowi o podanym nicku. W�tek w takiej sytuacji nie czeka na potwierdzenie odebrania wiadomo�ci.
 * 
 * 
 * @author Sebastian Pawe�oszek
 *
 */
public class FileSynchronizer implements Runnable {

    private String username;
    private String filename;
    private String friendname;
    private String directory;
    private ObjectInputStream objectReader;
    private ObjectOutputStream objectWriter;
    private ArrayList<String> clientFiles;
    private ArrayList<String> serverFiles;
    private ListView<String> usersList;
    private ArrayList<String> lackingClientFiles;
    private ArrayList<String> lackingServerFiles;
    private Button btn;
    private Label state;
    
    private boolean onlyShare = false; // flaga wykorzystywana kiedy chcemy tylko udostepnic plik
    
    /**
     * Konstruktor wykorzystywany do uruchomienia w�tku maj�cego dokona� synchronizacji plik�w pomi�dzy 
     * klientem, a serwerem.
     * @param username Nasz pseudonim po kt�rym jeste�my identyfikowani przez serwer.
     * @param directory Sciezka do folderu z naszymi plikami.
     * @param files ObservableList zawierajaca liste plik�w znajduj�cych sie w naszym folderze.
     * @param usersList ListView do kt�rego zostanie wpisana lista aktualnych u�ytkownik�w serwera.
     * @param btn Referencja do przycisku, kt�ry zostanie zablokowany na czas dzia�ania watku.
     * @param state Label, czyli pole tekstowe. Wpisujemy do niego aktualnie wykonywan� przez watek czynno��.
     */
    public FileSynchronizer(String username, String directory, ObservableList<String> files,
	    ListView<String> usersList, Button btn, Label state)
    {
	this.username = username;
	this.directory = directory;
	this.clientFiles = new ArrayList<String>(files);
	this.usersList = usersList;
	this.btn = btn;
	this.state = state;
    }
    
    
    /**
     * Konstruktor wykorzystywany do utworzenia watku w celu udostepnienia pliku innemy u�ytkownikowi.
     * @param username Nasz pseudonim po kt�rym jeste�my identyfikowani przez serwer.
     * @param friendname Pseudonim osoby kt�rej udostepniamy plik.
     * @param filename Nazwa udostepnianego pliku.
     * @param directory Sciezka do folderu z naszymi plikami.
     * @param state Label, czyli pole tekstowe. Wpisujemy do niego aktualnie wykonywan� przez watek czynno��.
     */
    public FileSynchronizer(String username, String friendname, String filename, String directory, Label state)
    {
	this.username = username;
	this.friendname = friendname;
	this.directory = directory;
	this.filename = filename;
	this.state = state;
	this.onlyShare = true;
    }
    
    @Override
    public void run()
    {
	try 
	{   
	    if(onlyShare)
	    {
		shareFile(filename);
		return;
	    }
	    
	    btn.setDisable(true);
	    serverFiles = downloadFilesList();
	    
	    lackingClientFiles = arrayDifference(serverFiles, clientFiles);
	    lackingServerFiles = arrayDifference(clientFiles, serverFiles);
	    
	    downloadAllFiles(lackingClientFiles);
	    uploadAllFiles(lackingServerFiles);
	    
	    downloadUsersList();
	    
	    print("Gotowosc do dzialania.");
	    
	    
	} 
	catch (CommunicationErrorException e) 
	{
	    print("Blad komunikacji z serwerem!");
	    return;
	}
	catch(UnexpectedException e)
	{
	    print("Aplikacja nie moze dzialac z nieznanych przyczyn!");
	    return;
	}
	finally
	{
	    btn.setDisable(false);
	}
	
    }//run
    
    /*
     * downloadFilesList()
     * -zwraca ArrayList<String>, zawierajaca liste plikow uzytkownika na serwerze
     */
    
    /**
     * Metoda nawi�zuj�ca po��czenie z serwerem i wysy�aj�ca Request
     *  z pro�b� o przys�anie listy plik�w z serwera, nale��cych do u�ytkownika. 
     *  Po otrzymaniu danych zamyka po��czenie.
     *  Wewn�trz metody znajduj� si� spowalniacze celowo wyd�u�aj�ce czas jej dzia�ania. Ich jedynym
     *  zastosowaniem jest lepsze ilutrowanie dzia�ania metody.
     * 
     * @return Zwraca ArrayList zawieraj�c� list� plik�w u�ytkonika znajduj�cych si� na serwerze.
     * @throws CommunicationErrorException Wyj�tek rzucany w sytuacji kiedy nie mo�na
     *  nawi�za� po��czenia z serwerem, albo co� je przerwa�o.
     * @throws UnexpectedException Wyj�tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo�liwo�ci otworzenia okre�lonego pliku.
     */
    @SuppressWarnings("unchecked")
    private ArrayList<String> downloadFilesList() throws CommunicationErrorException, UnexpectedException
    {
	try 
	{
	    print("Synchronizacja z serwerem...");
	    
	    mySleep(2000);
	    
	    /*
	     * Nawiazujemy polaczenie z serwerem.
	     */
	    
	    Socket socket = establishConnection();	
	    
	    /*
	     * Tworzymy obiekt request i wysylamy go do serwera.
	     * Czekamy na odpowiedz od serwera.
	     */
	    
	    objectWriter = new ObjectOutputStream(socket.getOutputStream());
	    Request r = new Request(username,"sync");
	    objectWriter.writeObject(r);
	    objectReader = new ObjectInputStream(socket.getInputStream());
	    ArrayList<String> n = (ArrayList<String>) objectReader.readObject();
	    
	    objectWriter.close();
	    objectReader.close();
	    socket.close();  
	    return n;
	} 
	catch (IOException e) 
	{
	    e.printStackTrace();
	    throw new CommunicationErrorException();
	} 
	catch (ClassNotFoundException e) 
	{
	    print("Nieobslugiwany sposob przesylu danych!");
	    throw new UnexpectedException();
	}
	
    }//downloadFilesList()
    
    /**
     * Metoda nawi�zuj�ca po��czenie z serwerem i wysy�aj�ca Request
     * z pro�b� o przys�anie listy u�ytkownik�w aktualnie korzystaj�cych z serwera. 
     * Po otrzymaniu danych zamyka po��czenie.
     * Wewn�trz metody znajduj� si� spowalniacze celowo wyd�u�aj�ce czas jej dzia�ania. Ich jedynym
     * zastosowaniem jest lepsze ilutrowanie dzia�ania metody.
     * 
     * @throws CommunicationErrorException Wyj�tek rzucany w sytuacji kiedy nie mo�na
     *  nawi�za� po��czenia z serwerem, albo co� je przerwa�o.
     * @throws UnexpectedException Wyj�tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo�liwo�ci otworzenia okre�lonego pliku.
     */
    @SuppressWarnings("unchecked")
    private void downloadUsersList() throws CommunicationErrorException, UnexpectedException
    {
	try 
	{
	    print("Pobieram liste uzytkownikow...");
	    
	    mySleep(3000);
	    
	    /*
	     * Nawiazujemy polaczenie z serwerem.
	     */
	    
	    Socket socket = establishConnection();	
	    
	    /*
	     * Tworzymy obiekt request i wysylamy go do serwera.
	     */
	    
	    objectWriter = new ObjectOutputStream(socket.getOutputStream());
	    Request r = new Request(username,"users");
	    objectWriter.writeObject(r);
	    
	    /*
	     * Czekamy na odpowiedz od serwera.
	     */
	    
	    objectReader = new ObjectInputStream(socket.getInputStream());
	    ArrayList<String> n = (ArrayList<String>) objectReader.readObject();
	    
	    /*
	     * Stworzenie ObservableList na podstawie arrayListy.
	     * JavaFX zmusza nas do uzywania ObservableList,
	     * ale nie mozemy przesylac jej przez ObjectInputStream.
	     * Dlatego do przesylania uzywamy ArrayList. 
	     */
	    
	    ObservableList<String> u = FXCollections.observableArrayList(n);
	   
	    /*
	     * Zaktualizowanie listy w glownym watku.
	     */
	    
	    Platform.runLater(new Runnable()
	    	{
		    @Override
		    public void run() {
			usersList.setItems(u);
		    }
		    
		});
	    
	    /*
	     * Zamykamy sockety.
	     */
	    
	    objectWriter.close();
	    objectReader.close();
	    socket.close();  
	} 
	catch (IOException e) 
	{
	    print("Blad IO!");
	    mySleep(3000);
	    throw new UnexpectedException();
	} 
	catch (ClassNotFoundException e) 
	{
	    print("Nieobslugiwany sposob przesylu danych!");
	    mySleep(3000);
	    throw new UnexpectedException();
	} 
    }
    
    /*
     * downloadFile()
     * Pobiera plik o okreslonej nazwie i zapisuje go w folderze uzytkownika.
     */
    
    /**
     * 
     * Jej zadaniem jest pobranie pliku o podanej nazwie i zapisanie go do folderu u�ytkownika.
     * Wysy�a Request o wys�anie pliku i czeka na jego przys�anie przez serwer.
     * Po otrzymaniu pliku zamyka po��czenie.
     * 
     * @param fileName Nazwa pliku jaki chcemy pobra� z serwera.
     * @throws CommunicationErrorException Wyj�tek rzucany w sytuacji kiedy nie mo�na
     *  nawi�za� po��czenia z serwerem, albo co� je przerwa�o.
     * @throws UnexpectedException Wyj�tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo�liwo�ci otworzenia okre�lonego pliku.
     */
    private void downloadFile(String fileName) throws CommunicationErrorException, UnexpectedException
    {
		
	/*
	 * Wysy�amy request i czekamy na przyslanie pliku.
	 */
	    
	try 
	{
	    print("Pobieram plik: " + fileName);
	    //Celowe spowolnienie dzialania
	    mySleep(3000);
	    
	    Socket socket = establishConnection();
	    
	    objectWriter = new ObjectOutputStream(socket.getOutputStream());
	    Request r = new Request(username, "download", fileName);
	    objectWriter.writeObject(r);
	    
	    
	    DataInputStream dis = new DataInputStream(socket.getInputStream());
	    FileOutputStream fos = new FileOutputStream(directory + "/" + fileName);
	    
	    int count;
	    byte[] bytes = new byte[8 * 1024];
	    
	    while ((count = dis.read(bytes)) > 0)
	    {
	      fos.write(bytes, 0, count);
	    }
	    
	    fos.close();
	    dis.close();
	    socket.close();
	    
	} 
	catch (IOException e) 
	{
	    print("Blad zapisu odbieranego pliku!");
	    mySleep(3000); 
	    throw new UnexpectedException();
	}
	
    }//downloadFile()
    
    /**
     * 
     * Jej zadaniem jest wys�anie pliku o podanej nazwie do na serwer.
     * Wysy�a Request o przyj�cie pliku, po otworzeniu strumienia rozpoczyna wysy�anie go.
     * Po zako�czeniu procesu wysy�ania zamyka po��czenie.
     * 
     * @param fileName Nazwa pliku jaki chcemy wys�a� na serwer.
     * @throws CommunicationErrorException Wyj�tek rzucany w sytuacji kiedy nie mo�na
     *  nawi�za� po��czenia z serwerem, albo co� je przerwa�o.
     * @throws UnexpectedException Wyj�tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo�liwo�ci otworzenia okre�lonego pliku.
     */
    private void uploadFile(String fileName) throws CommunicationErrorException, UnexpectedException
    {
	
	try
	{
	    print("Wysylam plik: " + fileName);
	  //Celowe spowolnienie dzialania
	    mySleep(3000);
	    Socket socket = establishConnection();
	    
	    /*
	     * Wysylamy request o odebranie.
	     */
	    
	    objectWriter = new ObjectOutputStream(socket.getOutputStream());
	    Request r = new Request(username, "upload", fileName);
	    objectWriter.writeObject(r);
	    
	    /*
	     * Wysylamy plik.
	     */
	    
	    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
	    FileInputStream fis = new FileInputStream(directory + "/" + fileName);
	    
	    byte[] bytes = new byte[8 * 1024];
		
	    int count;
	    while ((count = fis.read(bytes)) > 0)
	    {
		dos.write(bytes, 0, count);
	    }
		
	    objectWriter.close();
	    dos.close();
	    fis.close();
	    socket.close();

	}
	catch(IOException e)
	{
	    print("Blad odczytu wysylanego pliku!");
	    mySleep(3000);
	    throw new UnexpectedException();
	}
    }//uploadFile
    
    /**
     * Zadaniem tej metody jest udostepnienie naszego pliku innemu u�ytkownikowi serwera. 
     * Wysy�a ona Request o udostepnienie, po czym zamyka po��czenie.
     * Nie czeka na potwierdzenie odebrania Requesta.
     * 
     * @param fileName Nazwa udost�pnianego pliku.
     * @throws CommunicationErrorException Wyj�tek rzucany w sytuacji kiedy nie mo�na
     *  nawi�za� po��czenia z serwerem, albo co� je przerwa�o.
     * @throws UnexpectedException Wyj�tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo�liwo�ci otworzenia okre�lonego pliku.
     */
    private void shareFile(String fileName) throws CommunicationErrorException, UnexpectedException
    {
	try 
	{
	    print("Udostepniam plik : " + fileName);
	    
	    mySleep(3000);
	    
	    Socket socket = establishConnection();
	    
	    /*
	     * Wysylamy request o udostepnienie.
	     */
	    
	    objectWriter = new ObjectOutputStream(socket.getOutputStream());
	    Request r = new Request(username, "share", fileName, friendname);
	    objectWriter.writeObject(r);
	    
	    objectWriter.close();
	    socket.close();
	    
	} 
	catch(IOException e)
	{
	    print("Blad IO!");
	    mySleep(3000);
	    throw new UnexpectedException();
	}
	
    }//shareFile()
    
    /**
     * Metoda dzia�aj�ca podobnie do downloadFile(), jednak ta przyjmuje list� plik�w do pobrania,
     * a nie nazw� jednego pliku.
     * 
     * @param files Lista plikow do pobrania
     * @throws CommunicationErrorException Wyj�tek rzucany w sytuacji kiedy nie mo�na
     *  nawi�za� po��czenia z serwerem, albo co� je przerwa�o.
     * @throws UnexpectedException Wyj�tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo�liwo�ci otworzenia okre�lonego pliku.
     */
    private void downloadAllFiles(ArrayList<String> files) throws CommunicationErrorException, UnexpectedException
    {
	
	for(String s:files)
	{
	    downloadFile(s);
	}
	
    }//downloadAllFiles
    
    /**
     * Metoda dzia�aj�ca podobnie do uploadFile(), jednak ta przyjmuje list� plik�w do wys�ania,
     * a nie nazw� jednego pliku.
     * 
     * @param files Lista plikow do wys�ania
     * @throws CommunicationErrorException Wyj�tek rzucany w sytuacji kiedy nie mo�na
     *  nawi�za� po��czenia z serwerem, albo co� je przerwa�o.
     * @throws UnexpectedException Wyj�tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo�liwo�ci otworzenia okre�lonego pliku.
     */
    private void uploadAllFiles(ArrayList<String> files) throws CommunicationErrorException, UnexpectedException
    {
	
	for(String s:files)
	{
	    uploadFile(s);
	}
	
    }//uploadAllFiles
    
    /*
     * arrayDifference()
     * Zwraca tablice ktora jest wynikiem odejmowania arr1 - arr2.
     * Zwraca elementy ktore ma pierwsza, a druga nie.
     */
    /**
     * Metoda s�u��ca do utworzenia ArrayListy nazw plikow, kt�re znajduj� si� w jednej li�cie,
     * ale w drugiej ju� nie. Innymi s�owy, jest to wynik operacji arr1 - arr2.
     * @param arr1 Pierwsza lista, od jej zawarto�ci odejmujemy zawarto�� drugiej.
     * @param arr2 Druga lista, jej zawarto�� odejmujemy od zawarto�ci pierwszej.
     * @return Elementy ktore znajduje sie w pierwszej li�cie, ale nie znajduj� w drugiej.
     */
    private static ArrayList<String> arrayDifference(ArrayList<String> arr1, ArrayList<String> arr2)
    {
	ArrayList<String> n = new ArrayList<String>();
	
	for(String s:arr1)
	{
	    if(!arr2.contains(s))
	    {
		n.add(s);
	    }
	}
	
	return n;
    }//arrayDifference()
    
    /**
     * 
     * Metoda wykorzystywana do nawi�zania po��czenia z serwerem.
     * 
     * @return Socket z nawi�zanym po��czeniem.
     * @throws CommunicationErrorException Wyj�tek rzucany w sytuacji kiedy nie mo�na
     *  nawi�za� po��czenia z serwerem, albo co� je przerwa�o.
     * @throws UnexpectedException Wyj�tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo�liwo�ci otworzenia okre�lonego pliku.
     */
    private Socket establishConnection() throws CommunicationErrorException, UnexpectedException
    {
	try 
	{
	    Socket socket = new Socket("127.0.0.1", 5000);
	    return socket;
	} 
	catch (ConnectException e)
	{
	    throw new CommunicationErrorException();
	} 
	catch (UnknownHostException e)
	{
	    throw new CommunicationErrorException();
	} 
	catch (IOException e) 
	{
	    print("Blad IO!");
	    mySleep(3000);
	    throw new UnexpectedException();
	} 
    }
    
    /**
     * Metoda slu�y do wy�wietlenia aktualnie wykonywanej czynno�ci.
     * @param String kt�ry ma zosta� umieszczony w obiekcie Label.
     */
    private void print(String n)
    {
	Platform.runLater(new Runnable()
	{
	    @Override
	    public void run() {
		state.setText(n);
	    }
	    
	});
    }
    
    /**
     * Skr�cona wersja Thread.sleep();
     * @param Liczba milisekund na jak� ma zosta� u�piony w�tek.
     */
    private void mySleep(int n)
    {
	try 
	{
	    Thread.sleep(n);
	} 
	catch (InterruptedException e) 
	{
	    //NOT GONNA HAPPEN
	    e.printStackTrace();
	}
	
    }
    
    /**
     * Wyj�tek rzucany w przypadku b��du komunikacji pomi�dzy klientem, a serwerem.
     * @author Sebastian Pawe�oszek
     *
     */
    private class CommunicationErrorException extends Exception
    {
	private static final long serialVersionUID = 1L;

	public CommunicationErrorException()
	{
	    super("Blad komunikacji z serwerem!");
	}
	
    }
    
    /**
     * Wyj�tek rzucany w przypadku niespodziewanego b��du napotkanego przez aplikacj�.
     * @author Sebastian Pawe�oszek
     *
     */
    private class UnexpectedException extends Exception
    {
	private static final long serialVersionUID = 1L;

	public UnexpectedException()
	{
	    super("Nieznany blad!");
	}
	
    }

}//class
