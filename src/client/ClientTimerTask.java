package client;

import java.util.TimerTask;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

/**
 * Klasa przeznaczona do cyklicznego wykorzystywania przez Timer. Jej zadaniem jest tworzenie listy plik�w
 * aktualnie znajduj�cych si� w folderze u�ytkownika. W przypadku gdy w folderze pojawi si� nowy plik
 * aktywuje w�tek wysy�aj�cy go na serwer. Kiedy nie mo�e uzyska� dost�pu do folderu u�ytkownika, 
 * wy�witla napis "Brak dostepu do plik�w!".
 * @author Sebastian Pawe�oszek
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
     * Konstruktor przypisuj�cy argumenty wej�ciowe do zmiennych lokalnych.
     * @param filesList ListView z plikami u�ytkownika.
     * @param usersList	Lista u�ytkownik�w serwera.
     * @param files Lista plik�w u�ytkownika.
     * @param directory Lokalizacja folderu u�ytkownika.
     * @param username Nazwa u�ytkownika.
     * @param odswiezButton Przycisk od�wie�aj�cy liste plik�w.
     * @param state Etykieta informuj�ca o aktualnie wykonywanej czynno�ci.
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
			    state.setText("Brak dostepu do plik�w!");
			}

		    }
        }); 
	
    }

}
