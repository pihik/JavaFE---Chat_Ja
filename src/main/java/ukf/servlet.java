package ukf;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class servlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String URL = "jdbc:mysql://localhost/ais";
    private String LOGIN = "root";
    private String PWD = ""; 
    private Connection con = null;
       
    public servlet() {
        super();
    }
	public void init(ServletConfig config) throws ServletException {
		super.init();
        try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			con = DriverManager.getConnection(URL,LOGIN,PWD);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}
	public void destroy() {
		try {
			con.close();
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {	
		response.setContentType("Text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		try {
			if (con==null) {out.println("chyba spojenia"); return;}
			String operacia = request.getParameter("operacia");
			if (operacia==null) {zobrazNeopravnenyPristup(out); return; }
			if (operacia.equals("login")) { overUsera(out, request); }
			int user_id = getUserID(request);
			if (user_id==0) {zobrazNeopravnenyPristup(out); return; }
			vypisHlavicka(out, request);
			if (operacia.equals("logout")) {urobLogout(out,request); return; }
			if (operacia.equals("addMessage")) { AddMessage(out, request, user_id + ""); }
			ChatView(out, user_id);
		}
		catch (Exception e)
		{
			out.println(e.getMessage());
		}
	}
	void ChatView(PrintWriter out, Integer id)
	{
		out.println("<form method='get' action='servlet'>");
		out.println("<p><label for='chat'>Chat</label></p>");
		out.println("<textarea id='textarea' name='textarea' rows='20' cols='50' readonly>" + GetMessage(out) + "</textarea>");
		out.println("<input type='text' name='message'>");
		out.println("<input type='hidden' name='operacia' value='addMessage'>");
		out.println("<input type='submit' value='odoslat'>");
		out.println("</form>");
	}
	String GetMessage(PrintWriter out) {
	    String sprava = "";
	    try {
	        Statement stmt = con.createStatement();
	        String sql = "SELECT s.cas, u.meno, s.sprava FROM spravy s LEFT JOIN users u ON s.user_id = u.id";
	        ResultSet rs = stmt.executeQuery(sql);
	        
	        while(rs.next()) {
	            sprava += rs.getString("cas") + " | ";
	            sprava += rs.getString("meno") + " | ";
	            sprava += rs.getString("sprava") + "\n";
	        }
	        rs.close();
	        stmt.close();
	    } catch (Exception e) {
	        out.println(e.getMessage());
	    }
	    return sprava;
	}
	void AddMessage(PrintWriter out, HttpServletRequest request, String id) throws ServletException, IOException {
		String message = request.getParameter("message");
		if (message.length() > 50) { out.println("Sprava je prilis dlha!"); return; }
		
		LocalDate localDate = java.time.LocalDate.now();
        LocalTime localTime = java.time.LocalTime.now();
        String datum = localDate.toString();
        String cas = localTime.toString();
		try {
			String sql = "INSERT INTO spravy (user_id, sprava, cas, datum) VALUES (?, ?, ?, ?)";
			PreparedStatement pstmt = con.prepareStatement(sql);
			pstmt.setString(1, id);
			pstmt.setString(2, message);
			pstmt.setString(3, cas);
			pstmt.setString(4, datum);
			
			pstmt.executeUpdate();
		} catch (Exception e) {
			out.println(e.getMessage());
		}
	}
	protected void zobrazNeopravnenyPristup(PrintWriter out)
	{
		out.print("Nemas pravo tu byt...");
	}
	protected void vypisHlavicka(PrintWriter out, HttpServletRequest request) {
		HttpSession session = request.getSession();
		out.println("<div style='display: inline;'>");
		out.println("<h2>"+session.getAttribute("meno")+" "+
		session.getAttribute("priezvisko")+"</h2>");
		Logout_Button(out);
		out.println("</div>");
		out.println("<hr>");
	}
	protected void overUsera(PrintWriter out, HttpServletRequest request) {
		try {
			String meno = request.getParameter("login");
			String heslo = request.getParameter("pwd");
			Statement stmt = con.createStatement();
			String sql = "SELECT MAX(id) AS iid, COUNT(id) AS pocet FROM users"+
			" WHERE email='"+meno+"' AND passwd = '"+heslo+"'";
			ResultSet rs = stmt.executeQuery(sql);
			rs.next();
			HttpSession session = request.getSession();
			if (rs.getInt("pocet")==1) {
				sql = "SELECT id, meno, priezvisko FROM users WHERE email ='"+meno+"'";
				rs = stmt.executeQuery(sql);
				rs.next();
				session.setAttribute("ID", rs.getInt("id"));
				session.setAttribute("meno", rs.getString("meno"));
				session.setAttribute("priezvisko", rs.getString("priezvisko"));
			} else {
				out.println("Autorizacia sa nepodarila. Skontroluj prihlasovacie udaje.");
				session.invalidate();
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			out.println(e.getMessage());
		}
	}
	protected int getUserID(HttpServletRequest request) {
		HttpSession session = request.getSession();
		Integer id = (Integer) (session.getAttribute("ID"));
		if (id==null) id = 0;
		return id;
	}
	void Logout_Button(PrintWriter out) 
	{
		out.println("<form method='post' action='servlet'>");
		out.println("<input type='hidden' name='operacia' value='logout'>");
		out.println("<input type='submit' value='logout'>");
		out.println("</form>");
	}
	protected void urobLogout(PrintWriter out, HttpServletRequest request) {
		HttpSession session = request.getSession();
		session.invalidate();
		out.println("Pouzivatel odhlaseny<br>");
		out.println("<a href='index.html'>Domov</a>");
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
