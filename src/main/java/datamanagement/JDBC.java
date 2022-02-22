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
    public ResultSet dql(String query) throws SQLException {
        Statement st = null;
        try {
            st = con.createStatement();
        }catch (Exception e){
            System.out.println("Error While creating the query Statement");
        }
        assert st != null;
        return st.executeQuery(query);
    }
    public void dml(String query) throws ClassNotFoundException, SQLException {
        Statement st = null;
        try {
            st = con.createStatement();
        }catch (Exception e){
            System.out.println("Error While creating the query Statement");
        }
        assert st != null;
        st.executeUpdate(query);
    }
    public void close() {
        try{
            con.close();
        }catch (Exception e){
            System.out.println("Connection not Closed. \n Re-trying...");
            try {
                con.close();
            }catch (Exception e2){
                System.out.println("Connection not closed");
                e2.printStackTrace();
            }
        }
    }
}


