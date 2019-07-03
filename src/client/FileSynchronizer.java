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
 * Klasa zawiera implementacje wszystkich metod odpowiedzialnych za komunikacjê pomiêdzy klientem, a serwerem.
 * Implementuje ona interfejs Runnable, co oznacza ¿e klasa powinna zostaæ wykorzystana jako w¹tek.
 * Dzia³a na zasadzie wysy³ania Requestów do serwera.
 * Serwer odbiera obiekt typu "Request" i podejmuje okreœlone dzia³ania w zale¿noœci od zawartoœci tego obiektu.
 * W¹tek FileSynchronizer wykonuje tylko jedn¹ czynnoœæ w danym momencie,
 * np. prosi o listê plików danego u¿ytkownika na serwerze. Nie zrobi niczego innego dopóki nie otrzyma odpowiedzi
 * na ostatnio wys³any Request.
 * Mo¿e pracowaæ w dwóch trybach:
 * -Pierwszy to synchronizacja plikow pomiêdzy klientem, a serwerem.
 * Oznacza to, ¿e w¹tek FileSynchronizer najpierw poprosi serwer o listê plików znajduj¹cych siê na serwerze,
 * nastêpnie porówna j¹ z list¹ plików w folderze u¿ytkownika. Je¿eli na serwerze znajduj¹ siê pliki których
 * nie ma w folderze u¿ytkownika to zostan¹ one pobrane. Z kolei kiedy na serwerze brakuje plików znajduj¹cych
 * siê w folderze to zostan¹ one wys³ane. Na koñcu zostanie jeszcze wys³any Request o przys³anie listy
 * u¿ytkowników korzystaj¹cych z serwera.
 * -Drugi to udostêpnienie pliku. FileSynchronizer wysy³a Request o udostêpnienie pliku nale¿¹cego do nas,
 * innemu u¿ytkonikowi o podanym nicku. W¹tek w takiej sytuacji nie czeka na potwierdzenie odebrania wiadomoœci.
 * 
 * 
 * @author Sebastian Pawe³oszek
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
     * Konstruktor wykorzystywany do uruchomienia w¹tku maj¹cego dokonaæ synchronizacji plików pomiêdzy 
     * klientem, a serwerem.
     * @param username Nasz pseudonim po którym jesteœmy identyfikowani przez serwer.
     * @param directory Sciezka do folderu z naszymi plikami.
     * @param files ObservableList zawierajaca liste plików znajduj¹cych sie w naszym folderze.
     * @param usersList ListView do którego zostanie wpisana lista aktualnych u¿ytkowników serwera.
     * @param btn Referencja do przycisku, który zostanie zablokowany na czas dzia³ania watku.
     * @param state Label, czyli pole tekstowe. Wpisujemy do niego aktualnie wykonywan¹ przez watek czynnoœæ.
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
     * Konstruktor wykorzystywany do utworzenia watku w celu udostepnienia pliku innemy u¿ytkownikowi.
     * @param username Nasz pseudonim po którym jesteœmy identyfikowani przez serwer.
     * @param friendname Pseudonim osoby której udostepniamy plik.
     * @param filename Nazwa udostepnianego pliku.
     * @param directory Sciezka do folderu z naszymi plikami.
     * @param state Label, czyli pole tekstowe. Wpisujemy do niego aktualnie wykonywan¹ przez watek czynnoœæ.
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
     * Metoda nawi¹zuj¹ca po³¹czenie z serwerem i wysy³aj¹ca Request
     *  z proœb¹ o przys³anie listy plików z serwera, nale¿¹cych do u¿ytkownika. 
     *  Po otrzymaniu danych zamyka po³¹czenie.
     *  Wewn¹trz metody znajduj¹ siê spowalniacze celowo wyd³u¿aj¹ce czas jej dzia³ania. Ich jedynym
     *  zastosowaniem jest lepsze ilutrowanie dzia³ania metody.
     * 
     * @return Zwraca ArrayList zawieraj¹c¹ listê plików u¿ytkonika znajduj¹cych siê na serwerze.
     * @throws CommunicationErrorException Wyj¹tek rzucany w sytuacji kiedy nie mo¿na
     *  nawi¹zaæ po³¹czenia z serwerem, albo coœ je przerwa³o.
     * @throws UnexpectedException Wyj¹tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo¿liwoœci otworzenia okreœlonego pliku.
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
     * Metoda nawi¹zuj¹ca po³¹czenie z serwerem i wysy³aj¹ca Request
     * z proœb¹ o przys³anie listy u¿ytkowników aktualnie korzystaj¹cych z serwera. 
     * Po otrzymaniu danych zamyka po³¹czenie.
     * Wewn¹trz metody znajduj¹ siê spowalniacze celowo wyd³u¿aj¹ce czas jej dzia³ania. Ich jedynym
     * zastosowaniem jest lepsze ilutrowanie dzia³ania metody.
     * 
     * @throws CommunicationErrorException Wyj¹tek rzucany w sytuacji kiedy nie mo¿na
     *  nawi¹zaæ po³¹czenia z serwerem, albo coœ je przerwa³o.
     * @throws UnexpectedException Wyj¹tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo¿liwoœci otworzenia okreœlonego pliku.
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
     * Jej zadaniem jest pobranie pliku o podanej nazwie i zapisanie go do folderu u¿ytkownika.
     * Wysy³a Request o wys³anie pliku i czeka na jego przys³anie przez serwer.
     * Po otrzymaniu pliku zamyka po³¹czenie.
     * 
     * @param fileName Nazwa pliku jaki chcemy pobraæ z serwera.
     * @throws CommunicationErrorException Wyj¹tek rzucany w sytuacji kiedy nie mo¿na
     *  nawi¹zaæ po³¹czenia z serwerem, albo coœ je przerwa³o.
     * @throws UnexpectedException Wyj¹tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo¿liwoœci otworzenia okreœlonego pliku.
     */
    private void downloadFile(String fileName) throws CommunicationErrorException, UnexpectedException
    {
		
	/*
	 * Wysy³amy request i czekamy na przyslanie pliku.
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
     * Jej zadaniem jest wys³anie pliku o podanej nazwie do na serwer.
     * Wysy³a Request o przyjêcie pliku, po otworzeniu strumienia rozpoczyna wysy³anie go.
     * Po zakoñczeniu procesu wysy³ania zamyka po³¹czenie.
     * 
     * @param fileName Nazwa pliku jaki chcemy wys³aæ na serwer.
     * @throws CommunicationErrorException Wyj¹tek rzucany w sytuacji kiedy nie mo¿na
     *  nawi¹zaæ po³¹czenia z serwerem, albo coœ je przerwa³o.
     * @throws UnexpectedException Wyj¹tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo¿liwoœci otworzenia okreœlonego pliku.
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
     * Zadaniem tej metody jest udostepnienie naszego pliku innemu u¿ytkownikowi serwera. 
     * Wysy³a ona Request o udostepnienie, po czym zamyka po³¹czenie.
     * Nie czeka na potwierdzenie odebrania Requesta.
     * 
     * @param fileName Nazwa udostêpnianego pliku.
     * @throws CommunicationErrorException Wyj¹tek rzucany w sytuacji kiedy nie mo¿na
     *  nawi¹zaæ po³¹czenia z serwerem, albo coœ je przerwa³o.
     * @throws UnexpectedException Wyj¹tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo¿liwoœci otworzenia okreœlonego pliku.
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
     * Metoda dzia³aj¹ca podobnie do downloadFile(), jednak ta przyjmuje listê plików do pobrania,
     * a nie nazwê jednego pliku.
     * 
     * @param files Lista plikow do pobrania
     * @throws CommunicationErrorException Wyj¹tek rzucany w sytuacji kiedy nie mo¿na
     *  nawi¹zaæ po³¹czenia z serwerem, albo coœ je przerwa³o.
     * @throws UnexpectedException Wyj¹tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo¿liwoœci otworzenia okreœlonego pliku.
     */
    private void downloadAllFiles(ArrayList<String> files) throws CommunicationErrorException, UnexpectedException
    {
	
	for(String s:files)
	{
	    downloadFile(s);
	}
	
    }//downloadAllFiles
    
    /**
     * Metoda dzia³aj¹ca podobnie do uploadFile(), jednak ta przyjmuje listê plików do wys³ania,
     * a nie nazwê jednego pliku.
     * 
     * @param files Lista plikow do wys³ania
     * @throws CommunicationErrorException Wyj¹tek rzucany w sytuacji kiedy nie mo¿na
     *  nawi¹zaæ po³¹czenia z serwerem, albo coœ je przerwa³o.
     * @throws UnexpectedException Wyj¹tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo¿liwoœci otworzenia okreœlonego pliku.
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
     * Metoda s³u¿¹ca do utworzenia ArrayListy nazw plikow, które znajduj¹ siê w jednej liœcie,
     * ale w drugiej ju¿ nie. Innymi s³owy, jest to wynik operacji arr1 - arr2.
     * @param arr1 Pierwsza lista, od jej zawartoœci odejmujemy zawartoœæ drugiej.
     * @param arr2 Druga lista, jej zawartoœæ odejmujemy od zawartoœci pierwszej.
     * @return Elementy ktore znajduje sie w pierwszej liœcie, ale nie znajduj¹ w drugiej.
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
     * Metoda wykorzystywana do nawi¹zania po³¹czenia z serwerem.
     * 
     * @return Socket z nawi¹zanym po³¹czeniem.
     * @throws CommunicationErrorException Wyj¹tek rzucany w sytuacji kiedy nie mo¿na
     *  nawi¹zaæ po³¹czenia z serwerem, albo coœ je przerwa³o.
     * @throws UnexpectedException Wyj¹tek rzucany w przypadku nieprzewidzianych sytuacji takich
     * jak np. brak mo¿liwoœci otworzenia okreœlonego pliku.
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
     * Metoda slu¿y do wyœwietlenia aktualnie wykonywanej czynnoœci.
     * @param String który ma zostaæ umieszczony w obiekcie Label.
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
     * Skrócona wersja Thread.sleep();
     * @param Liczba milisekund na jak¹ ma zostaæ uœpiony w¹tek.
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
     * Wyj¹tek rzucany w przypadku b³êdu komunikacji pomiêdzy klientem, a serwerem.
     * @author Sebastian Pawe³oszek
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
     * Wyj¹tek rzucany w przypadku niespodziewanego b³êdu napotkanego przez aplikacjê.
     * @author Sebastian Pawe³oszek
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
