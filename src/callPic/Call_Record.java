package callPic;
import java.sql.*;
import java.util.ArrayList;

/**
 * Created by yellowsea on 2016/8/9.
 */
public class Call_Record {
    static Connection dbConn;
    static Statement sql;
    static ResultSet rst;
    public String UserID;
    public String UserName;
    public String PhoneID;
    public String Num;
    public Connection getConnection(){

        try{
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            dbConn=DriverManager.getConnection("jdbc:sqlserver://localhost:1433;DatabaseName=Test","sa","wz45683968");
        }catch(Exception e){
            e.printStackTrace();
        }
        return dbConn;
    }


    public static void main(String[] args){
        Call_Record c=new Call_Record();
        dbConn=c.getConnection();
        try{
            sql=dbConn.createStatement();//实例化Statement对象
            //执行SQL语句，返回结果集
            rst=sql.executeQuery("select * from dbo.Call_Record");
            while(rst.next()){
                //String user_id=rst.getString("user_id");//获取列名是“user_id”的字段值
                //String user_name=rst.getString("user_name");//获取列名是“user_name”的字段值
                //String phone_id=rst.getString("phone_id");//获取列名是“phone_id”的字段值
                //String phone_number=rst.getString("phone_number");//获取列名是“phone_number”的字段值
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static ArrayList<Node> transfer() {
        Call_Record c=new Call_Record();
        ArrayList<Node> nodeList = new ArrayList<>();
        dbConn=c.getConnection();
        try{
            sql=dbConn.createStatement();//实例化Statement对象
            //执行SQL语句，返回结果集
            rst=sql.executeQuery("select * from dbo.Call_Record");
            while(rst.next()){
                String UserID=rst.getString("user_id");//获取列名是“user_id”的字段值
                String UserName=rst.getString("user_name");//获取列名是“user_name”的字段值
                String PhoneID=rst.getString("phone_id");//获取列名是“phone_id”的字段值
                String Num=rst.getString("phone_number");//获取列名是“phone_number”的字段值
                Node node = new Node(UserID, UserName, PhoneID, Num);
                nodeList.add(node);
            }
        }catch(Exception e){
            e.printStackTrace();
        } finally {
            return nodeList;
        }
    }
}
