package datamanagement;

import java.sql.*;


public class JDBC {
    Connection con;

    public JDBC() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://localhost:5432/chat";
        this.con = DriverManager.getConnection(url, username, password);
    }

    String username = "postgres";
    String password = "@TeslaPostgresql2000";

    public ResultSet dql(String query) throws SQLException, ClassNotFoundException {
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        //con.close();
        return rs;
    }

    public void dml(String query) throws ClassNotFoundException, SQLException {
        Statement st = con.createStatement();
        st.executeUpdate(query);
        //con.close();
    }

    public void addUser(String query) throws ClassNotFoundException, SQLException {
        Statement st = con.createStatement();
        st.executeUpdate(query);
        //con.close();
    }
}


