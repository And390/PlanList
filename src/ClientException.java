/**
 * And390 - 21.03.15.
 * Ќеправильные действи€ со стороны клиента.
 */
public class ClientException extends Exception
{
    public int statusCode;

    public ClientException(String message)  {  super(message);  statusCode=400;  }
    public ClientException(String message, int statusCode_)  {  super(message);  statusCode=statusCode_;  }
}
