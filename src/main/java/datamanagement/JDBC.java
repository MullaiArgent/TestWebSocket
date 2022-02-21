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
}


