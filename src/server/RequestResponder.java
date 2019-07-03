package server;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;

import javafx.application.Platform;
import javafx.scene.control.Label;
import request.Request;

/**
 * Klasa s³u¿¹ca do stworzenia w¹tku. Pobiera z kolejki Requesty i obs³uguje je na podstawie ich zawartoœci.
 * Potrafi wys³aæ listê plików nale¿¹cych do danego u¿ytkownika, wysy³aæ listê u¿ytkowników
 * aktualnie korzystaj¹cych z serwera, wysy³aæ za¿¹dane pliki, odbieraæ pliki i zapisywaæ je w odpowiednim
 * miejscu, oraz udostêpniaæ pliki innym u¿ytkownikom.
 * @author Sebastian Pwae³oszek
 *
 */
public class RequestResponder implements Runnable {

    private BlockingQueue<Request> taskQueue;
    private ObjectOutputStream objectWriter;
    private Request r;
    private boolean on = true;
    private Label state;
    
    RequestResponder(BlockingQueue<Request> taskQueue, Label state)
    {
	this.taskQueue = taskQueue;
	this.state = state;
    }
    
    @Override
    public void run() {

	while(on)
	{
		try 
		{
		    print("Gotowy");
		    r = taskQueue.take();
		    
		    if(!on)
		    {
			break;
		    }
		    
		    if(r.getAction().equals("sync"))
		    {
			syncRespond();
		    }
		    else if(r.getAction().equals("users"))
		    {
			usersRespond();
		    }
		    else if(r.getAction().equals("download"))
		    {
			downloadRespond();
		    }
		    else if(r.getAction().equals("upload"))
		    {
			uploadRespond();
		    }
		    else if(r.getAction().equals("share"))
		    {
			shareRespond();
		    }
		    
		} 
		catch (InterruptedException e) 
		{
		    e.printStackTrace();
		}
		catch (IOException e)
		{
		    e.printStackTrace();
		}
		catch (Exception e)
		{
		    e.printStackTrace();
		}
		
	}
	
    }
    
    private void syncRespond() throws IOException, InterruptedException
    {
	
	print("Wysy³am liste plikow dla " + r.getUsername());
	Thread.sleep(3000);
	
	objectWriter = new ObjectOutputStream(r.getSocket().getOutputStream());
	objectWriter.writeObject( CsvReader.readUserFiles(r.getUsername() ));
	
	objectWriter.close();
	r.getSocket().close();
    }

    private void usersRespond() throws IOException, InterruptedException
    {
	
	print("Wysy³am liste u¿ytkowników.");
	Thread.sleep(3000);
	
	objectWriter = new ObjectOutputStream(r.getSocket().getOutputStream());
	objectWriter.writeObject( CsvReader.listUsers());
	
	objectWriter.close();
	r.getSocket().close();
    }
    
    private void downloadRespond() throws IOException, InterruptedException
    {
	
	print("Wysy³am " + r.getNeededFile() + " do " + r.getUsername());
	Thread.sleep(3000);
	
	String loc = "files/" + CsvReader.localiseFile(r.getUsername(), r.getNeededFile()) + "/" + r.getNeededFile();
	DataOutputStream dos = new DataOutputStream(r.getSocket().getOutputStream());
	FileInputStream fis = new FileInputStream(loc);
	
	byte[] bytes = new byte[8 * 1024];
	
	int count;
        while ((count = fis.read(bytes)) > 0) 
        {
            dos.write(bytes, 0, count);
        }
	
        dos.close();
        fis.close();
        r.getSocket().close();
    }
    
    private void uploadRespond() throws IOException, InterruptedException
    {
	
	print("Odbieram " + r.getNeededFile() + " od " + r.getUsername());
	Thread.sleep(3000);
	
	String disc = CsvReader.chooseDisc();
	String loc = "files/" + disc + "/" + r.getNeededFile();
	DataInputStream dis = new DataInputStream(r.getSocket().getInputStream());
	FileOutputStream fos = new FileOutputStream(loc);
	
	int count;
	byte[] bytes = new byte[8 * 1024];
	    
	while ((count = dis.read(bytes)) > 0)
	{
	    fos.write(bytes, 0, count);
	}
	
	CsvReader.addRecord(r.getUsername(),r.getNeededFile(),disc);
	
	fos.close();
	dis.close();
	r.getSocket().close();
	
    }
    
    private void shareRespond() throws IOException, InterruptedException
    {

	print("Udostepniam " + r.getNeededFile() + " dla " + r.getFriend());
	Thread.sleep(3000);
	
	if( !CsvReader.readUserFiles(r.getFriend()).contains(r.getNeededFile()) )
	{
	    CsvReader.addRecord(r.getFriend(), r.getNeededFile(),
		    CsvReader.localiseFile(r.getUsername(), r.getNeededFile()));
	}
	
	r.getSocket().close();
    }
    
    /**
     * Metoda slu¿y do wyœwietlenia aktualnie wykonywanej czynnoœci.
     * @param n Tekst który zostanie wypisany na ekranie.
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
     * Metoda s³u¿¹ca do bezpiecznego zakoñczenia dzia³ania w¹tku.
     */
    public void shutdown()
    {
	on = false;
	
	try
	{
	    taskQueue.add(new Request());
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	}
    }
    
}
