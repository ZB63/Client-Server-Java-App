package server;


import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import request.Request;

/**
 * Aplikacja serwerowa s³u¿¹ca jedynie do wyœwietlania zawartoœci poszczególnych dysków i 
 * czynnoœci któr¹ zajête s¹ poszczególne w¹tki obs³uguj¹ce klientów.
 * Do prawid³owego funkcjonowania wymaga, aby w folderze z aplikacj¹ znajdowa³ siê folder o nazwie
 * "files", a w nim foldery o nazwach "1", "2", "3", "4" i "5". W ka¿dym z piêciu folderów musi
 * istnieæ plik o nazwie "book.csv". Foldery te symuluj¹ dyski.
 * @author Sebastian Pawe³oszek
 * @version 1.0
 */
public class ServerMain extends Application {

    Timer timer = new Timer();
    Scene scene;//tu wrzuce wszystko co bedzie na naszym ekranie
    BlockingQueue<Request> taskQueue = new LinkedBlockingQueue<Request>();
    Label state1 = new Label("Uruchamianie...");
    Label state2 = new Label("Uruchamianie...");
    
    RequestReceiver receiver = new RequestReceiver(taskQueue);
    
    RequestResponder responder = new RequestResponder(taskQueue,state1);
    RequestResponder responder2 = new RequestResponder(taskQueue,state2);
    
    
    
    //Listy plikow na poszczegolnych serwerach
    ObservableList<String> files1;
    ObservableList<String> files2;
    ObservableList<String> files3;
    ObservableList<String> files4;
    ObservableList<String> files5;
    
    /**
     * Metoda wywo³ywana jest wtedy kiedy chcemy wymusiæ zatrzymanie aplikacji.
     * Wykonuje ona czynnoœci które musz¹ byæ zrobione przed zamkniêciem aplikacji.
     * Metoda ta jest wywo³ywana w w¹tku JavaFX.
     */
    @Override
    public void stop(){
	    System.out.println("Stage is closing");
	    receiver.shutdown();
	    responder.shutdown();
	    responder2.shutdown();
	    timer.cancel();
    }
    
    /**
     * Metoda jest punktem wejœciowym dla aplikajci JavaFX.
     * Odpowiada ona za interfejs graficzny, zachowanie siê aplikacji po naciœniêciu okreœlonych przycisków
     * i cykliczne odœwie¿anie listy plikow na dyskach.
     */
    @Override
    public void start(Stage primaryStage)
    {
	//Tworzymy glowny poziomy panel
	HBox root = new HBox(5);
	
	state1.setMinHeight(40);
	state1.setMinWidth(300);
	state1.setFont(new Font("Cambria", 16));
	
	state2.setMinHeight(40);
	state2.setMinWidth(300);
	state2.setFont(new Font("Cambria", 16));
	
	//Vertical1
	VBox vertical1 = new VBox(5);
	vertical1.setPadding(new Insets(5));
	Label label1 = new Label("Dysk 1");
	ListView<String> filesList1 = new ListView<>();
	
	filesList1.setPrefWidth(150);
	filesList1.setPrefHeight(300);
	
	vertical1.getChildren().addAll(label1,filesList1);
	
	//Vertical2
	VBox vertical2 = new VBox(5);
	vertical2.setPadding(new Insets(5));
	Label label2 = new Label("Dysk 2");
	ListView<String> filesList2 = new ListView<>();
	
	filesList2.setPrefWidth(150);
	filesList2.setPrefHeight(300);
	
	vertical2.getChildren().addAll(label2,filesList2);
	
	//Vertical3
	VBox vertical3 = new VBox(5);
	vertical3.setPadding(new Insets(5));
	Label label3 = new Label("Dysk 3");
	ListView<String> filesList3 = new ListView<>();
	
	filesList3.setPrefWidth(150);
	filesList3.setPrefHeight(300);
	
	vertical3.getChildren().addAll(label3,filesList3);
	
	//Vertical4
	VBox vertical4 = new VBox(5);
	vertical4.setPadding(new Insets(5));
	Label label4 = new Label("Dysk 4");
	ListView<String> filesList4 = new ListView<>();
	
	filesList4.setPrefWidth(150);
	filesList4.setPrefHeight(300);
	
	vertical4.getChildren().addAll(label4,filesList4);
	
	//Vertical5
	VBox vertical5 = new VBox(5);
	vertical5.setPadding(new Insets(5));
	Label label5 = new Label("Dysk 5");
	ListView<String> filesList5 = new ListView<>();
	
	filesList5.setPrefWidth(150);
	filesList5.setPrefHeight(300);
	
	vertical5.getChildren().addAll(label5,filesList5);
	
	//Verical6
	VBox vertical6 = new VBox(5);
	vertical6.setPadding(new Insets(5));
	Label label6 = new Label("Panel kontrolny");
	Label t1 = new Label("Thread 1:");
	Label t2 = new Label("Thread 2:");
	
	t1.setFont(new Font("Cambria", 16));
	t2.setFont(new Font("Cambria", 16));
	
	Button zakonczButton = new Button("Zakoncz");
	
	vertical6.setPrefWidth(150);
	vertical6.setPrefHeight(300);
	
	zakonczButton.setOnAction(new EventHandler<ActionEvent>(){
	    @Override
	    public void handle(ActionEvent event) {
		Platform.exit();
	    }
	});
	
	vertical6.getChildren().addAll(label6,zakonczButton, t1, state1, t2, state2);
	
	//Dodajemy wszystko do glownego panelu
	root.getChildren().addAll(vertical1,vertical2,vertical3,vertical4,vertical5,vertical6);

	
	//Uaktualniamy liste plikow co 5 sekund
	timer.scheduleAtFixedRate(new TimerTask() {

	    @Override
	    public void run() {
		
		ObservableList<String> results1 = FolderReader.readFolder("files\\1");
		ObservableList<String> results2 = FolderReader.readFolder("files\\2");
		ObservableList<String> results3 = FolderReader.readFolder("files\\3");
		ObservableList<String> results4 = FolderReader.readFolder("files\\4");
		ObservableList<String> results5 = FolderReader.readFolder("files\\5");
		
		Platform.runLater(new Runnable() {
		    @Override
		    public void run()
			    {
				files1 = results1;
				filesList1.setItems(files1);
				files2 = results2;
				filesList2.setItems(files2);
				files3 = results3;
				filesList3.setItems(files3);
				files4 = results4;
				filesList4.setItems(files4);
				files5 = results5;
				filesList5.setItems(files5);
				
			    }
	        }); 
		
	    }
	    
	},1,1*1000);
	
	//Tworzenie sceny i wyswietlenie jej
	scene = new Scene(root);
	primaryStage.setTitle("Serwer");
	primaryStage.setScene(scene);
	primaryStage.setOpacity(0.98);
	primaryStage.show();
	
	//Watek odbieraj¹cy requesty
	
	Thread tReceiver = new Thread(receiver);
	tReceiver.start();
	
	//Dwa watki wysylajacy dane
	Thread tResponder = new Thread(responder);
	Thread tResponder2 = new Thread(responder2);
	tResponder.start();
	tResponder2.start();
	
    }
    
    public static void main(String[] args) {
	launch(args);
    }
}
