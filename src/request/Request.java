package request;

import java.io.Serializable;
import java.net.Socket;

/**
 * Klasa wykorzystywana przez klienta do ��dania od serwera okre�lonych czynno�ci.
 * Klient wysy�a obiekt Request do serwera, a ten w odpowiedzi podejmuje 
 * czynno�ci opisane wewn�trz obiektu Request. 
 * Kody wykorzystywane przez klas� Request:
 * "sync" - ��danie listy plik�w u�ytkownika na serwerze,
 * "download" - ��danie wys�ania pliku,
 * "upload" - ��danie odebrania pliku,
 * "share" - ��danie udost�pnienia pliku,
 * "users" - ��danie wys�ania listy u�ytkownik�w serwera.
 * @author Sebastian Pawe�oszek
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
     * Konstruktor wykorzystywany przez metod� shutdown() wewn�trz klasy RequestResponder,
     * w celu sztucznego zapchania kolejki.
     * Nie ma innych zastosowa�.
     */
    public Request(){}
    
    /**
     * Konstruktor wykorzystywany przy tworzeniu obiektu do ��dania listy plik�w i u�ytkownik�w.
     * @param username Pseudonim po kt�rym jeste�my identyfikowani przez serwer.
     * @param action Dla listy plik�w Action = "sync", dla listy u�ytkownik�w "users".
     */
    public Request(String username, String action)
    {
	this.username = username;
	this.action = action;
    }
    
    /**
     * Konstruktor wykorzystywany przy ��daniu wys�ania, albo odebrania pliku.
     * @param username Pseudonim po kt�rym jeste�my identyfikowani przez serwer.
     * @param action Dla �adania wys�ania pliku Action = "download", dla �adanie odebrania "upload".
     * @param neededFile Nazwa pliku, kt�ry ma zosta� wys�any, albo odebrany.
     */
    public Request(String username, String action, String neededFile)
    {
	this.username = username;
	this.action = action;
	this.neededFile = neededFile;
    }
    
    /**
     * Konstruktor wykorzystywany przy ��daniu udost�pnienia pliku innemu u�ytkownikowi.
     * @param username Pseudonim po kt�rym jeste�my identyfikowani przez serwer.
     * @param action Dla udost�pnienia pliku Action = "share".
     * @param neededFile Nazwa pliku, kt�ry ma zosta� udost�pniony.
     * @param friend Nazwa u�ytkownika, kt�remu plik zostanie udost�pniony.
     */
    public Request(String username, String action, String neededFile, String friend)
    {
	this.username = username;
	this.action = action;
	this.neededFile = neededFile;
	this.friend = friend;
    }
    
    /**
     * Przypisuje socket do zmiennej lokalnej. Konieczne aby Request m�g� by� prawid�owo obs�u�ony.
     * @param clientSocket Obiekt Socket do zapisania w zmiennej lokalnej.
     */
    public void setSocket(Socket clientSocket)
    {
	this.clientSocket = clientSocket;
    }
    
    /**
     * Zwraca rodzaj czynno�ci kt�rej za��da� tw�rca Requesta.
     * @return Czynno�� za��dana przez tw�rc� Requesta.
     */
    public String getAction()
    {
	return this.action;
    }
    
    /**
     * Zwraca nazw� u�ytkonika, kt�ry utworzy� Request.
     * @return Nazwa u�ytkownika.
     */
    public String getUsername()
    {
	return this.username;
    }
    
    /**
     * Zwraca nazw� u�ytkownika kt�remu ma zosta� udost�pniony plik. Zwraca null w przypadku gdy 
     * Request nie mia� w polu action warto�ci "share".
     * @return Nazwa u�ytkownika kt�remu udost�pniamy plik.
     */
    public String getFriend()
    {
	return this.friend;
    }
    
    /**
     * Zwraca nazw� pliku, wpisan� do Requesta. W zale�no�ci od rodzaju Requesta jest to plik
     * kt�ry ma zosta� wys�any lub odebrany przez serwer.
     * @return Nazwa pliku.
     */
    public String getNeededFile()
    {
	return this.neededFile;
    }
    
    /**
     * Zwraca obiekt typu Socket, mo�na go wykorzysta� do udzielenia odpowiedzi tw�rcy Requesta.
     * Metoda zwr�ci null, je�eli wywo�ujemy j� wewn�trz aplikacji klienckiej.
     * @return Obiekt typu Socket, s�u��cy do udzielania odpowiedzi klientowi.
     */
    public Socket getSocket()
    {
	return this.clientSocket;
    }
}
