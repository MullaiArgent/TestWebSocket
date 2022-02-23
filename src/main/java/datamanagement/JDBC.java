package datamanagement;

import java.sql.*;


public class JDBC {
    Connection con;
    String username = "postgres";
    String password = "@TeslaPostgresql2000";
    public JDBC() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://localhost:5432/chat";
        this.con = DriverManager.getConnection(url, username, password);
    }
    public ResultSet dql(String query) throws SQLException, ClassNotFoundException {
        Statement st = con.createStatement();
        return st.executeQuery(query);
    }
    public void dml(String query) throws ClassNotFoundException, SQLException {
        Statement st = con.createStatement();
        st.executeUpdate(query);
    }
    public void close(){
        try {
            con.close();
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres trouble in closing the Connection \n Retrying...");
            try{
                con.close();
            }catch (Exception e1){
                e1.printStackTrace();
                System.out.println("Failed to Close the Connection");
            }
        }
    }
}


