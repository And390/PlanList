package planlist;

/**
 * And390 - 09.03.15.
 * ������������ �������� ������������ - ����� ������ ������������ ������������.
 */
public class UserException extends ClientException
{
    public UserException(String message)  {  super(message, true);  }
    public UserException(String message, int statusCode)  {  super(message, true, statusCode);  }
}
