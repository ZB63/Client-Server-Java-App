package server;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import request.Request;

/**
 * Klasa s³u¿¹ca do stworzenia w¹tku. Czeka na po³¹czenie od klienta, po czym przyjmuje
 * od niego Request i umieszcza go kolejce, gdzie czeka a¿ zostanie obs³u¿ony przez inny w¹tek.
 * @author Sebastian Pawe³oszek
 *
 */
public class RequestReceiver implements Runnable {
    
    private BlockingQueue<Request> taskQueue;
    private ServerSocket serverSocket;
    private boolean state = true;
    
    /**
     * Konstruktor, poza tym ¿e przypisuje referencjê kolejki do lokalnej zmiennej, tworzy obiekt
     * ServerSocket na porcie 5000.
     * @param taskQueue Referencja do kolejki przechowuj¹cej Requesty od klientow.
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
     * Odbiera po³¹czenia od klientów i uruchamia w¹tek zajmuj¹cy siê dalsz¹ obs³ug¹ po³¹czenia.
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
     * Metoda pozwalaj¹ca na bezpieczne zakoñczenie dzia³ania w¹tku.
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
     * Klasa s³u¿¹ca do stworzenia w¹tku. Ma za zadanie odczytaæ przesy³any Request, do³¹czyæ do niego
     * obiekt Socket i umieœciæ ca³oœæ w kolejce gdzie bêdzie oczekiwa³o na dalsz¹ obs³ugê.
     * @author Sebastian Pawe³oszek
     *
     */
    public class RequestHandler implements Runnable
    {
	ObjectInputStream objectReader;
	Socket clientSocket;
	Request r;
	
	/**
	 * Konstruktor przypisuje argument do zmiennej lokalnej o tej samej nazwie.
	 * @param clientSocket Otwarty socket umo¿liwiaj¹cy ³¹cznoœæ z klientem.
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
