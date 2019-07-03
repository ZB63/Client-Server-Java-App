package server;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Klasa dostarczaj¹ca metody do dokonywanie operacji na plikach .csv zawieraj¹cych informacje na temat
 * przechowywanych danych i ich w³aœcicieli.
 * @author Sebastian Pawe³oszek
 *
 */
public class CsvReader {

    static int disc = 5;
    static final String directory = "files";
    
    /**
     * Metoda odczytuje pliki csv we wszystkich piêciu folderach i zwraca listê plików nale¿¹cych do 
     * okreœlonego u¿ytkownika.
     * @param username U¿ytkownik którego pliki chcemy odnalezæ na serwerze.
     * @return Lista plików nale¿¹cych do u¿ytkownika.
     */
    public static ArrayList<String> readUserFiles(String username)
    {
	ArrayList<String> files = new ArrayList<String>();
	
	for(int i=1;i<6;i++)
	{
	    try
	    {
		BufferedReader br = openToRead(directory,i);
                String line;
           	    
           	    while ((line = br.readLine()) != null) 
           	    {
           		String[] values = line.split(",");
           		if(values[0].equals(username))
           		{
           		    files.add(values[1]);
            		}
           	    }
           	    
           	 br.close();
            } 
	    catch (FileNotFoundException e) 
	    {
		System.out.println("Plik nie istnieje!");
	    }
	    catch (IOException e1) 
	    {
		System.out.println("B³¹d odczytu!");
		e1.printStackTrace();
            }
	}
	//System.out.println(files);
	return files;
    }
    
    /**
     * Sprawdza na którym dysku znajduje siê plik o okreœlonej nazwie i w³aœcicielu.
     * @param username W³aœciciel pliku.
     * @param fileName Nazwa szukanego pliku.
     * @return Numer dysku, czyli "1", "2", "3", "4" lub "5". W przypadku kiedy plik nie istnieje zwraca null.
     */
    public static String localiseFile(String username, String fileName)
    {
	for(int i=1;i<6;i++)
	{
	    try
	    {
		BufferedReader br = openToRead(directory,i);
		String line;
           	    
		while ((line = br.readLine()) != null) 
		{
		    String[] values = line.split(",");
		    if(values[0].equals(username) && values[1].equals(fileName))
		    {
			return Integer.toString(i);
		    }
		}
		
		br.close();
            } 
	    catch (FileNotFoundException e) 
	    {
		System.out.println("Plik nie istnieje!");
	    }
	    catch (IOException e1) 
	    {
		System.out.println("B³¹d odczytu!");
		e1.printStackTrace();
            }
	}
	
	return null;
    }
    
    /**
     * Tworzy liste wszystkich u¿ytkownikow korzystaj¹cych z serwera.
     * @return Lista u¿ytkowników korzystaj¹cych z serwera.
     */
    public static ArrayList<String> listUsers()
    {
	ArrayList<String> users = new ArrayList<String>();
	
	for(int i=1;i<6;i++)
	{
	    try
	    {
		BufferedReader br = openToRead(directory,i);
                String line;
           	    
           	    while ((line = br.readLine()) != null) 
           	    {
           		String[] values = line.split(",");
           		if(!users.contains(values[0]))
           		{
           		    users.add(values[0]);
            		}
           	    }
           	    
           	 br.close();
            }  
	    catch (FileNotFoundException e) 
	    {
		System.out.println("Plik nie istnieje!");
	    }
	    catch (IOException e1) 
	    {
		System.out.println("B³¹d odczytu!");
		e1.printStackTrace();
            }
	}
	
	return users;
    }
    
    /**
     * S³u¿y do wybrania najodpowiedniejszego dysku do zapisu nowych danych.
     * @return Zwraca string z nazw¹ dysku, czyli "1", "2", "3", "4" lub "5".
     */
    public static synchronized String chooseDisc()
    {
	
	if(++disc > 5) disc = 1;
	
	return Integer.toString(disc);
    }
    
    /**
     * Dodaje do pliku csv wpis zawieraj¹cy nazwe nowego pliku i jego w³aœciciela.
     * @param userName Nazwa w³aœciciela pliku.
     * @param fileName Nazwa pliku.
     * @param disc Dysk na jakim plik ma zostaæ zapisany.
     */
    public static void addRecord(String userName, String fileName, String disc)
    {
	try 
	{
	    BufferedWriter bw = new BufferedWriter(new FileWriter("files/" + disc + "/book.csv",true));
	    bw.write(userName + "," + fileName);
	    bw.newLine();
	    bw.close();
	} 
	catch (IOException e) 
	{
	    e.printStackTrace();
	}
	
    }
    
    /**
     * Metoda wykorzystywana przez inne metody do otwierania pliku csv.
     * @param i Numer dysku.
     * @return Otwarty plik, lub null je¿eli nie uda³o siê otworzyæ.
     */
    public static BufferedReader openToRead(String directory, int i) throws FileNotFoundException
    {
	BufferedReader br;
	

	br = new BufferedReader(new FileReader(directory + "/" + Integer.toString(i) + "/book.csv"));
	return br;

    }
    
}
