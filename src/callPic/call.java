package callPic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Created by yellowsea on 2016/8/8.
 */
public class call {
    static  Connection conn;
    static Statement sql;
    static ResultSet rst;
    public String SID;
    public String ReID;
    public String ConID;
    public String Fre;
    public Connection getConnection(String path){
        try{
            Class.forName("org.sqlite.JDBC");
            conn=DriverManager.getConnection("jdbc:sqlite:" + path);
        }catch(Exception e){
            e.printStackTrace();
        }
        return conn;
    }

    public static void main(String[] args){
        call c=new call();
        conn=c.getConnection(args[1]);
        try{
            sql=conn.createStatement();//实例化Statement对象
            //执行sql语句，返回结果集
            rst=sql.executeQuery("select * from dbo.call");
            while(rst.next()){
                String SID=rst.getString("source_id");//获取列名是“source_id”的字段值
                String ReID=rst.getString("target_id");
                String ConID=rst.getString("call_id");
                String Fre=rst.getString("call_fre");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static ArrayList<Edge> transfer(){
        call c=new call();
        ArrayList<Edge> edgeList=new ArrayList<>();
        conn=c.getConnection(null);
        try{
            sql=conn.createStatement();
            rst=sql.executeQuery("select * from dbo.call");
            while(rst.next()){
                String SID=rst.getString("source_id");
                String ReID=rst.getString("target_id");
                String ConID=rst.getString("call_id");
                String Fre=rst.getString("call_fre");
                Edge edge=new Edge(ConID,SID,ReID,Fre);
                edgeList.add(edge);
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            return edgeList;
        }
    }


}
