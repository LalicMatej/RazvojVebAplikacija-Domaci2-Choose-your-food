package org.raflab.domaci2;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet(name = "AdminServlet", urlPatterns = {"/odabrana-jela"})
public class AdminServlet extends HttpServlet {

    private String adminPassword = "admin";

    @Override
    public void init() throws ServletException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("password.txt");

        if (is == null) {
            System.out.println("UPOZORENJE: Ne mogu da nadjem password.txt u resources folderu. Koristim default lozinku.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                adminPassword = line.trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String inputPassword = req.getParameter("lozinka");

        String css = "<style>"
                + "body { font-family: sans-serif; color: #000; padding: 20px; }"
                + ".container { max-width: 700px; margin: 0 auto; border: 1px solid #ccc; padding: 20px; }"
                + "h2 { border-bottom: 1px solid #ccc; padding-bottom: 5px; }"
                + "table { width: 100%; border-collapse: collapse; margin-top: 10px; }"
                + "th, td { padding: 8px; border: 1px solid #ccc; text-align: left; }"
                + "th { background-color: #eee; }"
                + "select { padding: 5px; margin-bottom: 15px; display: block; }"
                + "button { padding: 8px 16px; cursor: pointer; }"
                + ".back-link { display: block; margin-top: 20px; color: #000; text-decoration: underline; }"
                + "</style>";

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html><html><head><title>Admin Statistika</title>" + css + "</head><body>");
        out.println("<div class='container'>");

        if (inputPassword == null || !inputPassword.equals(adminPassword)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.println("<h2 class='error'> Pristup odbijen</h2>");
            out.println("<p style='text-align:center;'>Uneli ste pogrešnu lozinku.</p>");
            out.println("<a href='index.html' class='back-link'>← Pokušajte ponovo</a>");
            out.println("</div></body></html>");
            return;
        }

        out.println("<h2>Statistika Porudžbina </h2>");

        out.println("<form action='odabrana-jela' method='POST' onsubmit='return confirm(\"Da li ste sigurni da želite da obrišete sve porudžbine?\");'>");
        out.println("<input type='hidden' name='lozinka' value='" + inputPassword + "'>");
        out.println("<input type='hidden' name='action' value='clear'>");
        out.println("<button type='submit' class='btn-danger'> Očisti sve porudžbine</button>");
        out.println("</form>");

        ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> orders =
                (ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>>) getServletContext().getAttribute("orders");

        if (orders != null) {
            for (Map.Entry<String, ConcurrentHashMap<String, AtomicInteger>> dayEntry : orders.entrySet()) {
                out.println("<h3>" + dayEntry.getKey() + "</h3>");
                out.println("<table>");
                out.println("<tr><th style='width: 10%;'>#</th><th>Jelo</th><th style='width: 25%;'>Količina</th></tr>");

                int rb = 1;
                for (Map.Entry<String, AtomicInteger> mealEntry : dayEntry.getValue().entrySet()) {
                    out.println("<tr>");
                    out.println("<td>" + (rb++) + "</td>");
                    out.println("<td>" + mealEntry.getKey() + "</td>");
                    out.println("<td><b>" + mealEntry.getValue().get() + "</b></td>");
                    out.println("</tr>");
                }
                out.println("</table>");
            }
        } else {
            out.println("<p>Nema unetih jela u sistem.</p>");
        }

        out.println("<a href='index.html' class='back-link'>← Nazad na početnu</a>");
        out.println("</div></body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String inputPassword = req.getParameter("lozinka");

        if (inputPassword == null || !inputPassword.equals(adminPassword)) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if ("clear".equals(req.getParameter("action"))) {
            ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> orders =
                    (ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>>) getServletContext().getAttribute("orders");

            if (orders != null) {
                for (ConcurrentHashMap<String, AtomicInteger> dayOrders : orders.values()) {
                    for (AtomicInteger count : dayOrders.values()) {
                        count.set(0);
                    }
                }
            }

            Integer counter = (Integer) getServletContext().getAttribute("resetCounter");
            getServletContext().setAttribute("resetCounter", counter + 1);
        }

        resp.sendRedirect("odabrana-jela?lozinka=" + inputPassword);
    }
}