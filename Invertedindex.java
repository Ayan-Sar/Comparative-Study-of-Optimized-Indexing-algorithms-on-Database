import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;

public class Invertedindex {
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/minor", "root", "mysql");
    }

    private static void addToIndex(Map<String, Map<Integer, Integer>> index, String term, int docID, int position) {
        Map<Integer, Integer> posting = index.get(term);
        if (posting == null) {
            posting = new HashMap<>();
            index.put(term, posting);
        }
        posting.put(docID, position);
    }

    private static Map<String, Map<Integer, Integer>> buildIndex() throws SQLException {
        Map<String, Map<Integer, Integer>> index = new HashMap<>();
        Connection connection = getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM invertedindex");

        while (resultSet.next()) {
            int docID = resultSet.getInt("DocID");
            String text = resultSet.getString("Text");
            String[] terms = text.split("\\s+");
            int position = 0;

            for (String term : terms) {
                addToIndex(index, term, docID, position);
                position++;
            }
        }

        statement.close();
        connection.close();
        return index;
    }

    private static void insertDocument(String text) throws SQLException {
        Connection connection = getConnection();
        String insertQuery = "INSERT INTO invertedindex (Text) VALUES (?)";
        PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);
        preparedStatement.setString(1, text);
        preparedStatement.executeUpdate();
        preparedStatement.close();
        connection.close();
    }

    private static void deleteDocument(int docID) throws SQLException {
        Connection connection = getConnection();
        String deleteQuery = "DELETE FROM invertedindex WHERE DocID = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery);
        preparedStatement.setInt(1, docID);
        preparedStatement.executeUpdate();
        preparedStatement.close();
        connection.close();
    }

    public static void main(String[] args) throws SQLException, IOException {
        long startTime, endTime;

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        startTime = System.currentTimeMillis();
        Map<String, Map<Integer, Integer>> index = buildIndex();
        endTime = System.currentTimeMillis();
        dataset.addValue(endTime - startTime, "Time", "Index Build");

        String textToInsert = "This is a sample document for insertion.";
        startTime = System.currentTimeMillis();
        insertDocument(textToInsert);
        endTime = System.currentTimeMillis();
        dataset.addValue(endTime - startTime, "Time", "Insertion");

        int docIDToDelete = 1;
        startTime = System.currentTimeMillis();
        deleteDocument(docIDToDelete);
        endTime = System.currentTimeMillis();
        dataset.addValue(endTime - startTime, "Time", "Deletion");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter a search term:");
        String searchTerm = reader.readLine();

        startTime = System.currentTimeMillis();
        Map<Integer, Integer> posting = index.get(searchTerm);
        endTime = System.currentTimeMillis();

        if (posting == null) {
            System.out.println("No results found for: " + searchTerm);
        } else {
            System.out.println("Results for: " + searchTerm);
            for (Map.Entry<Integer, Integer> entry : posting.entrySet()) {
                System.out.println("DocID: " + entry.getKey() + ", Position: " + entry.getValue());
            }
        }
        dataset.addValue(endTime - startTime, "Time", "Query Response");

        JFreeChart chart = ChartFactory.createBarChart(
                "Time Analysis",
                "Operation", "Time (ms)",
                dataset, PlotOrientation.VERTICAL, true, true, false);

        ChartPanel chartPanel = new ChartPanel(chart);
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(chartPanel);
        frame.pack();
        frame.setVisible(true);
    }
}
