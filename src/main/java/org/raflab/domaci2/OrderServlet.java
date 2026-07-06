package org.raflab.domaci2;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet(name = "OrderServlet", urlPatterns = {"/order"})
public class OrderServlet extends HttpServlet {

    private Map<String, List<String>> menu = new LinkedHashMap<>();
    private ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> orders = new ConcurrentHashMap<>();

    @Override
    public void init() throws ServletException {
        String[] days = {"ponedeljak", "utorak", "sreda", "cetvrtak", "petak"};

        for (String day : days) {
            List<String> meals = new ArrayList<>();

            InputStream is = getClass().getClassLoader().getResourceAsStream(day + ".txt");

            if (is == null) {
                System.out.println("GRESKA: Ne mogu da nadjem fajl " + day + ".txt u resources folderu!");
                continue;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        meals.add(line.trim());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String dan = day.substring(0, 1).toUpperCase() + day.substring(1);
            menu.put(dan, meals);

            ConcurrentHashMap<String, AtomicInteger> dayOrders = new ConcurrentHashMap<>();
            for (String meal : meals) {
                dayOrders.put(meal, new AtomicInteger(0));
            }
            orders.put(dan, dayOrders);
        }

        getServletContext().setAttribute("menu", menu);
        getServletContext().setAttribute("orders", orders);
        getServletContext().setAttribute("resetCounter", 0);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        HttpSession session = req.getSession();
        Integer orderVersion = (Integer) session.getAttribute("orderVersion");
        Integer currentVersion = (Integer) getServletContext().getAttribute("resetCounter");

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
        out.println("<!DOCTYPE html><html><head><title>Odabir jela</title>" + css + "</head><body>");
        out.println("<div class='container'>");

        if (orderVersion != null && orderVersion.equals(currentVersion)) {
            out.println("<h2 class='success'> Porudžbina je zabeležena!</h2>");
            out.println("<p style='text-align:center;'>Vaš izbor u ovoj sesiji je bio:</p><ul>");

            Map<String, String> userOrder = (Map<String, String>) session.getAttribute("userOrder");
            if (userOrder != null) {
                for (Map.Entry<String, String> entry : userOrder.entrySet()) {
                    out.println("<li><b>" + entry.getKey() + ":</b> " + entry.getValue() + "</li>");
                }
            }
            out.println("</ul><a href='index.html' class='back-link'>← Nazad na početnu</a>");
            out.println("</div></body></html>");
            return;
        }

        out.println("<h2>Odaberite vaš ručak </h2>");
        out.println("<form action='order' method='POST'>");

        for (Map.Entry<String, List<String>> entry : menu.entrySet()) {
            String dan = entry.getKey();
            out.println("<label>" + dan + "</label>");
            out.println("<select name='" + dan + "' required>");
            out.println("<option value='' disabled selected>-- Izaberite jelo --</option>");

            for (String jelo : entry.getValue()) {
                out.println("<option value='" + jelo + "'>" + jelo + "</option>");
            }
            out.println("</select>");
        }

        out.println("<button type='submit'>Potvrdite unos</button>");
        out.println("</form>");
        out.println("<a href='index.html' class='back-link'>← Nazad</a>");
        out.println("</div></body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession();
        Integer currentVersion = (Integer) getServletContext().getAttribute("resetCounter");
        Map<String, String> userOrder = new LinkedHashMap<>();

        for (String day : menu.keySet()) {
            String selectedMeal = req.getParameter(day);
            if (selectedMeal != null && !selectedMeal.isEmpty()) {
                userOrder.put(day, selectedMeal);

                ConcurrentHashMap<String, AtomicInteger> dayOrders = orders.get(day);
                if (dayOrders != null && dayOrders.containsKey(selectedMeal)) {
                    dayOrders.get(selectedMeal).incrementAndGet();
                }
            }
        }

        session.setAttribute("orderVersion", currentVersion);
        session.setAttribute("userOrder", userOrder);
        resp.sendRedirect("order");
    }
}