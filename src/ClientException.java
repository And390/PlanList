/**
 * And390 - 21.03.15.
 * ������������ �������� �� ������� �������.
 */
public class ClientException extends Exception
{
    public boolean user = false;  //������������ �������� ������������ (� �� ����������� ����� �������)
    public int statusCode = 400;

    public ClientException(String message)  {  super(message);  }
    public ClientException(String message, boolean user_)  {  super(message);  user=user_;  }
    public ClientException(String message, int statusCode_)  {  super(message);  statusCode=statusCode_;  }
    public ClientException(String message, boolean user_, int statusCode_)  {  super(message);  user=user_;  statusCode=statusCode_;  }

    public String getUserMessage()  {  return user ? getMessage() : "Client request error";  }
}
