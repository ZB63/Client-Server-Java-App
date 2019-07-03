package client;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Klasa odpowiedzialna za odczytywanie zawartosci okreœlonego folderu. Posiada tylko jedn¹ statyczn¹ metodê.
 * Nie ma sytuacji która wymaga³a by utworzenia instancji obiektu FolderReader.
 * @author Sebastian Pawe³oszek
 *
 */
public class FolderReader {

    /**
     * Przeszukuje wskazany folder i listuje znalezione w nim pliki. Nie przeszukuje podfolderów i nie traktuje
     * ich jak pliki.
     * @param directory Lokalizacja folderu do przeszukania.
     * @param state informacja o bledzie, która zostanie wyœwietlona w interfejscie graficznym
     * @return Lista plikow znajdujacych sie w przeszukiwanym folderze.
     */
    public static ObservableList<String> readFolder(String directory)
    {
	
	ObservableList<String> results = FXCollections.observableArrayList();
	String s;
	
	try (Stream<Path> list = Files.list(Paths.get(directory))) 
        {
        	List<String> result;
        	result = list.filter(Files::isRegularFile).map(x -> x.toString()).collect(Collectors.toList());
        
        	for(int i=0;i<result.size();i++)
        	{
        	    s = result.get(i).replace(directory + "\\" , "");
        	    results.add(s);
        	}
         	    
        } 
	catch(NoSuchFileException e)
	{
	    System.out.println("Nie mo¿na odnalzæ dysków!");
	    return null;
	}
	catch(InvalidPathException e)
	{
	    System.out.println("Nie mozna uzyskac dostepu do dyskow, sprawdz czy sciezka jest poprawna!");
	    return null;
	} 
	catch (IOException e) 
	{
	    System.out.println("Wystapil problem z dostêpem do plików!");
	    return null;
	}

	return results; 
    }
    
}
