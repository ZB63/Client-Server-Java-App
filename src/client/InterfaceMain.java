package client;
	
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
/**
 * Aplikacja kliencka s³u¿¹ca do przesy³ania plików pomiêdzy
 * u¿ytkownikiem, a serwerem. Przy uruchomieniu wymaga podania dwóch argumentów,
 * nazwy u¿ytkownika i œcie¿ki do folderu.
 * @author Sebastian Pawe³oszek
 * @version 1.0
 */
public class InterfaceMain extends Application {
    	
    Scene scene;//ekran glowny
    Label state  = new Label("Gotowy");
    static String directory = "";//sciezka do obserwowanego folderu
    static String username = "student";
    ObservableList<String> files = FolderReader.readFolder(directory);
    ObservableList<String> users;
    ObservableList<String> filesOnServer;
    Timer timer = new Timer();
    
    /**
     * Metoda wywo³ywana jest wtedy kiedy chcemy wymusiæ zatrzymanie aplikacji.
     * Wykonuje ona czynnoœci które musz¹ byæ zrobione przed zamkniêciem aplikacji.
     * Metoda ta jest wywo³ywana w w¹tku JavaFX.
     */
    @Override
    public void stop(){
	    System.out.println("Stage is closing");
	    timer.cancel();
    }
    
    /**
     * Metoda jest punktem wejœciowym dla aplikajci JavaFX.
     * Odpowiada ona za interfejs graficzny, zachowanie siê aplikacji po naciœniêciu okreœlonych przycisków
     * i cykliczne odœwie¿anie listy plikow w folderze.
     */
    @Override
    public void start(Stage primaryStage) {
	try {
	    	//Stan programu
		state.setFont(new Font("Cambria", 16));
		state.setPrefHeight(50);
		
		
		//Glowny panel
		HBox root = new HBox(5);
			
			
		//Vertical 1
		VBox vertical1 = new VBox(5);
		vertical1.setPadding(new Insets(5));
		Label label1 = new Label("Moje pliki");
			
		ListView<String> filesList = new ListView<>();

		filesList.setPrefWidth(200);
		filesList.setPrefHeight(300);
			
		vertical1.getChildren().addAll(label1,filesList);
			
			
		//Vertical 2
		VBox vertical2 = new VBox(5);
		vertical2.setPadding(new Insets(5));
		Label label2 = new Label("Uzytkownicy");
		
		ListView<String> usersList = new ListView<>();
			
		usersList.setPrefWidth(200);
		usersList.setPrefHeight(150);
			
		vertical2.getChildren().addAll(label2,usersList,state);
			
			
		//Vertical 3
		VBox vertical3 = new VBox(5);
		vertical3.setPadding(new Insets(5));
			
		Label label3 = new Label("Przyciski");
			
		Button odswiezButton = new Button("Odswiez");
		Button udostepnijButton = new Button("Udostepnij");
		Button zakonczButton = new Button("Zakoncz");
						
		vertical3.getChildren().addAll(label3,odswiezButton,udostepnijButton,zakonczButton);
			
		odswiezButton.setOnAction(new EventHandler<ActionEvent>(){
		    @Override
		    public void handle(ActionEvent event) {
			
			Thread t = new Thread(new FileSynchronizer(username,directory,files,
				usersList,odswiezButton,state));
			
			t.start();
		    }
		});
			
		udostepnijButton.setOnAction(new EventHandler<ActionEvent>(){
		    @Override
		    public void handle(ActionEvent event) {
			
			Thread t = new Thread(new FileSynchronizer(username,usersList.getSelectionModel().getSelectedItem(),
				filesList.getSelectionModel().getSelectedItem(), directory, state));
			
			t.start();
			
			udostepnijButton.setDisable(true);
			
			(new Timer()).schedule(new TimerTask()
				 {
				    @Override
				    public void run() {
					udostepnijButton.setDisable(false);
					
				    }	     
			},2000);
			
		    }
		});
			
		zakonczButton.setOnAction(new EventHandler<ActionEvent>(){
		    @Override
		    public void handle(ActionEvent event) {
			Platform.exit();
		    }
		});
			
		root.getChildren().addAll(vertical1,vertical2,vertical3);
			
		//Koniec glownego panelu
		
		
		
		//Cykliczne aktualizowanie stanu plikow
		timer.scheduleAtFixedRate( new ClientTimerTask(filesList, usersList, files, directory,
			username, odswiezButton, state), 0, 3000);//aktualizacja co 3s
		
		
		timer.schedule(new TimerTask()
		{
		    
		    @Override
		    public void run() {
			if(files != null)
			{
			    Thread t = new Thread(new FileSynchronizer(username,directory,files,
				    usersList,odswiezButton,state));
        				
			    t.start();
			}else
			{
			    state.setText("Nieprawidlowa sciezka!");
			    odswiezButton.setDisable(true);
			    udostepnijButton.setDisable(true);
			}
		    }
		    
		}, 1000);
		
		
		scene = new Scene(root,550,350);

		primaryStage.setTitle("Klient");
		primaryStage.setScene(scene);
		primaryStage.setOpacity(0.98);
		primaryStage.show();
			
		
	} 
	catch(NullPointerException e) 
	{
	    state.setText("Nie mozna uzyskaæ dostepu do folderow!");
	    try 
	    {
		Thread.sleep(5000);
	    } 
	    catch (InterruptedException e1) 
	    {
		e1.printStackTrace();
	    }
	    stop();
	}
    }
	
    public static void main(String[] args) {
	try
	{
	    username = args[0];
	    directory = args[1];
	}
	catch(ArrayIndexOutOfBoundsException e)
	{
	    System.out.println("Podaj argumenty wejsciowe!");
	}
	
	launch(args);
    }
}
