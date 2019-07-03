package request;

import java.io.Serializable;
import java.net.Socket;

/**
 * Klasa wykorzystywana przez klienta do ¿¹dania od serwera okreœlonych czynnoœci.
 * Klient wysy³a obiekt Request do serwera, a ten w odpowiedzi podejmuje 
 * czynnoœci opisane wewn¹trz obiektu Request. 
 * Kody wykorzystywane przez klasê Request:
 * "sync" - ¿¹danie listy plików u¿ytkownika na serwerze,
 * "download" - ¿¹danie wys³ania pliku,
 * "upload" - ¿¹danie odebrania pliku,
 * "share" - ¿¹danie udostêpnienia pliku,
 * "users" - ¿¹danie wys³ania listy u¿ytkowników serwera.
 * @author Sebastian Pawe³oszek
 *
 */
public class Request implements Serializable{

    /*
     Bedzie kilka rodzajow requestow(zmienna action).
     
     1.Dej mie liste plikow na serwerze - "sync"
     	-nie trzeba nic wpisywac do nazwy pliku(zmienna neededFile)
     	
     2.Daj mi konkretny plik - "download"
     	-trzeba podac nazwe pliku w polu nazwy pliku(zmienna neededFile)
     	
     3.Wez ode mnie konkretny plik - "upload"
     	-trzeba podac nazwe pliku w polu nazwy pliku(zmienna neededFile)
     	
     4.Udostepnij plik innemu uzytkownikowi - "share"
     	-trzeba podac nazwe pliku w polu nazwy pliku(zmienna neededFile)
     	-trzeba podac nazwe uzytkownika ktoremu udostepniamu plik
     	
     5.Daj liste uzytkownikow serwera - "users"
     	-nie trzeba nic wpisywac do nazwy pliku(zmienna neededFile)
     */
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String username;
    private String friend;
    private String action;
    private String neededFile;
    private Socket clientSocket;
    
    /**
     * Konstruktor wykorzystywany przez metodê shutdown() wewn¹trz klasy RequestResponder,
     * w celu sztucznego zapchania kolejki.
     * Nie ma innych zastosowañ.
     */
    public Request(){}
    
    /**
     * Konstruktor wykorzystywany przy tworzeniu obiektu do ¿¹dania listy plików i u¿ytkowników.
     * @param username Pseudonim po którym jesteœmy identyfikowani przez serwer.
     * @param action Dla listy plików Action = "sync", dla listy u¿ytkowników "users".
     */
    public Request(String username, String action)
    {
	this.username = username;
	this.action = action;
    }
    
    /**
     * Konstruktor wykorzystywany przy ¿¹daniu wys³ania, albo odebrania pliku.
     * @param username Pseudonim po którym jesteœmy identyfikowani przez serwer.
     * @param action Dla ¿adania wys³ania pliku Action = "download", dla ¿adanie odebrania "upload".
     * @param neededFile Nazwa pliku, który ma zostaæ wys³any, albo odebrany.
     */
    public Request(String username, String action, String neededFile)
    {
	this.username = username;
	this.action = action;
	this.neededFile = neededFile;
    }
    
    /**
     * Konstruktor wykorzystywany przy ¿¹daniu udostêpnienia pliku innemu u¿ytkownikowi.
     * @param username Pseudonim po którym jesteœmy identyfikowani przez serwer.
     * @param action Dla udostêpnienia pliku Action = "share".
     * @param neededFile Nazwa pliku, który ma zostaæ udostêpniony.
     * @param friend Nazwa u¿ytkownika, któremu plik zostanie udostêpniony.
     */
    public Request(String username, String action, String neededFile, String friend)
    {
	this.username = username;
	this.action = action;
	this.neededFile = neededFile;
	this.friend = friend;
    }
    
    /**
     * Przypisuje socket do zmiennej lokalnej. Konieczne aby Request móg³ byæ prawid³owo obs³u¿ony.
     * @param clientSocket Obiekt Socket do zapisania w zmiennej lokalnej.
     */
    public void setSocket(Socket clientSocket)
    {
	this.clientSocket = clientSocket;
    }
    
    /**
     * Zwraca rodzaj czynnoœci której za¿¹da³ twórca Requesta.
     * @return Czynnoœæ za¿¹dana przez twórcê Requesta.
     */
    public String getAction()
    {
	return this.action;
    }
    
    /**
     * Zwraca nazwê u¿ytkonika, który utworzy³ Request.
     * @return Nazwa u¿ytkownika.
     */
    public String getUsername()
    {
	return this.username;
    }
    
    /**
     * Zwraca nazwê u¿ytkownika któremu ma zostaæ udostêpniony plik. Zwraca null w przypadku gdy 
     * Request nie mia³ w polu action wartoœci "share".
     * @return Nazwa u¿ytkownika któremu udostêpniamy plik.
     */
    public String getFriend()
    {
	return this.friend;
    }
    
    /**
     * Zwraca nazwê pliku, wpisan¹ do Requesta. W zale¿noœci od rodzaju Requesta jest to plik
     * który ma zostaæ wys³any lub odebrany przez serwer.
     * @return Nazwa pliku.
     */
    public String getNeededFile()
    {
	return this.neededFile;
    }
    
    /**
     * Zwraca obiekt typu Socket, mo¿na go wykorzystaæ do udzielenia odpowiedzi twórcy Requesta.
     * Metoda zwróci null, je¿eli wywo³ujemy j¹ wewn¹trz aplikacji klienckiej.
     * @return Obiekt typu Socket, s³u¿¹cy do udzielania odpowiedzi klientowi.
     */
    public Socket getSocket()
    {
	return this.clientSocket;
    }
}
