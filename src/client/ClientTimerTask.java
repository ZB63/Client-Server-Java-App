package client;

import java.util.TimerTask;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

/**
 * Klasa przeznaczona do cyklicznego wykorzystywania przez Timer. Jej zadaniem jest tworzenie listy plików
 * aktualnie znajduj¹cych siê w folderze u¿ytkownika. W przypadku gdy w folderze pojawi siê nowy plik
 * aktywuje w¹tek wysy³aj¹cy go na serwer. Kiedy nie mo¿e uzyskaæ dostêpu do folderu u¿ytkownika, 
 * wyœwitla napis "Brak dostepu do plików!".
 * @author Sebastian Pawe³oszek
 *
 */
public class ClientTimerTask extends TimerTask{

    ListView<String> filesList;
    ListView<String> usersList;
    ObservableList<String> files;
    Button odswiezButton;
    String username;
    String directory;
    Label state;
    
    /**
     * Konstruktor przypisuj¹cy argumenty wejœciowe do zmiennych lokalnych.
     * @param filesList ListView z plikami u¿ytkownika.
     * @param usersList	Lista u¿ytkowników serwera.
     * @param files Lista plików u¿ytkownika.
     * @param directory Lokalizacja folderu u¿ytkownika.
     * @param username Nazwa u¿ytkownika.
     * @param odswiezButton Przycisk odœwie¿aj¹cy liste plików.
     * @param state Etykieta informuj¹ca o aktualnie wykonywanej czynnoœci.
     */
    public ClientTimerTask(ListView<String> filesList,ListView<String> usersList, ObservableList<String> files,
	    String directory, String username, Button odswiezButton, Label state)
    {
	this.filesList = filesList;
	this.usersList = usersList;
	this.files = files;
	this.username = username;
	this.directory = directory;
	this.odswiezButton = odswiezButton;
	this.state = state;
	
    }
    
    @Override
    public void run() {
	
	ObservableList<String> results = FolderReader.readFolder(directory);
	
	Platform.runLater(new Runnable() {
	    @Override
	    public void run()
		    {
		
			try
			{
			    if(!files.containsAll(results))
			    {
				files = results;
				filesList.setItems(files);
				Thread t = new Thread(new FileSynchronizer(username,directory,files,
					usersList,odswiezButton,state));
                			
				t.start();
			    }
			    else
			    {
				files = results;
				filesList.setItems(files);
			    }
			}
			catch(NullPointerException e)
			{
			    state.setText("Brak dostepu do plików!");
			}

		    }
        }); 
	
    }

}
