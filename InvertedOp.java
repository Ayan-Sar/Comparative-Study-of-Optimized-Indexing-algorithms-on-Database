import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;

public class InvertedOp {
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/minor", "root", "mysql");
    }

    private static void addToIndex(Map<String, Map<Integer, Integer>> index, String term, int docID, int position) {
        Map<Integer, Integer> posting = index.computeIfAbsent(term, k -> new HashMap<>());
        posting.put(docID, position);
    }

    private static Map<String, Map<Integer, Integer>> buildIndex() throws SQLException {
        Map<String, Map<Integer, Integer>> index = new HashMap<>();
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT DocID, Text FROM invertedindex")) {

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
        }
        return index;
    }

    private static void insertDocuments(List<String> texts) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO invertedindex (Text) VALUES (?)")) {

            for (String text : texts) {
                preparedStatement.setString(1, text);
                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();
        }
    }

    private static void deleteDocument(int docID) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM invertedindex WHERE DocID = ?")) {

            preparedStatement.setInt(1, docID);
            preparedStatement.executeUpdate();
        }
    }

    public static void main(String[] args) throws SQLException, IOException {
        long startTime, endTime;

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        startTime = System.currentTimeMillis();
        Map<String, Map<Integer, Integer>> index = buildIndex();
        endTime = System.currentTimeMillis();
        dataset.addValue(endTime - startTime, "Time", "Index Build");

        List<String> documentsToInsert = new ArrayList<>();
        documentsToInsert.add("This is a sample document for insertion.");
        documentsToInsert.add("Another document for insertion.");
        startTime = System.currentTimeMillis();
        insertDocuments(documentsToInsert);
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
