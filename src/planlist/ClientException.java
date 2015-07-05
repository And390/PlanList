package planlist;

import utils.ExternalException;

/**
 * And390 - 21.03.15.
 * Ќеправильные действи€ со стороны клиента.
 */
public class ClientException extends ExternalException
{
    public boolean user = false;  //Ќеправильные действи€ пользовател€ (а не программной части клиента)
    public int statusCode = 400;

    public static int NOT_FOUND = 404;
    public static int FORBIDDEN = 403;

    public ClientException(String message)  {  super(message);  }
    public ClientException(String message, boolean user_)  {  super(message);  user=user_;  }
    public ClientException(String message, int statusCode_)  {  super(message);  statusCode=statusCode_;  }
    public ClientException(String message, boolean user_, int statusCode_)  {  super(message);  user=user_;  statusCode=statusCode_;  }

    public String getUserMessage()  {  return user ? getMessage() : "Client request error";  }
}
