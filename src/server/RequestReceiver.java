package server;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import request.Request;

/**
 * Klasa s�u��ca do stworzenia w�tku. Czeka na po��czenie od klienta, po czym przyjmuje
 * od niego Request i umieszcza go kolejce, gdzie czeka a� zostanie obs�u�ony przez inny w�tek.
 * @author Sebastian Pawe�oszek
 *
 */
public class RequestReceiver implements Runnable {
    
    private BlockingQueue<Request> taskQueue;
    private ServerSocket serverSocket;
    private boolean state = true;
    
    /**
     * Konstruktor, poza tym �e przypisuje referencj� kolejki do lokalnej zmiennej, tworzy obiekt
     * ServerSocket na porcie 5000.
     * @param taskQueue Referencja do kolejki przechowuj�cej Requesty od klientow.
     */
    public RequestReceiver(BlockingQueue<Request> taskQueue)
    {
	this.taskQueue = taskQueue;
	try
	{
	    serverSocket = new ServerSocket(5000);
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	}
	
    }
    
    /**
     * Odbiera po��czenia od klient�w i uruchamia w�tek zajmuj�cy si� dalsz� obs�ug� po��czenia.
     */
    @Override
    public void run() {
	
	while(state) {
		
		try 
		{
		    Socket clientSocket = serverSocket.accept();
		    if(state)
		    {
			Thread t = new Thread(new RequestHandler(clientSocket));
			t.start();
		    }
		    
		    
		} 
		catch(Exception ex) 
		{
		    ex.printStackTrace();
		}
		
	}
	
    }

    /**
     * Metoda pozwalaj�ca na bezpieczne zako�czenie dzia�ania w�tku.
     */
    public void shutdown()
    {
	state = false;
	try 
	{
	    new Socket(serverSocket.getInetAddress(),serverSocket.getLocalPort()).close();
	} 
	catch (IOException e)
	{
	    e.printStackTrace();
	}
    }
    
    /**
     * Klasa s�u��ca do stworzenia w�tku. Ma za zadanie odczyta� przesy�any Request, do��czy� do niego
     * obiekt Socket i umie�ci� ca�o�� w kolejce gdzie b�dzie oczekiwa�o na dalsz� obs�ug�.
     * @author Sebastian Pawe�oszek
     *
     */
    public class RequestHandler implements Runnable
    {
	ObjectInputStream objectReader;
	Socket clientSocket;
	Request r;
	
	/**
	 * Konstruktor przypisuje argument do zmiennej lokalnej o tej samej nazwie.
	 * @param clientSocket Otwarty socket umo�liwiaj�cy ��czno�� z klientem.
	 */
	public RequestHandler(Socket clientSocket)
	{
	    this.clientSocket = clientSocket;
	}
	
	@Override
	public void run() {
	    
	    try
	    {
		objectReader = new ObjectInputStream(clientSocket.getInputStream());
		
		r = (Request) objectReader.readObject();
		r.setSocket(clientSocket);
		
		System.out.println("Request od: " + r.getUsername());
		
		while(!taskQueue.offer(r));
		
	    }
	    catch(Exception e)
	    {
		e.printStackTrace();
	    }
	    
	
	}
	
    }
    
}
