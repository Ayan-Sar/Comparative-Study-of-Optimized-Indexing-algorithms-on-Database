import java.sql.*;
import java.util.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class Bitmapindex extends ApplicationFrame {
    private static final String URL = "jdbc:mysql://localhost:3306/minor";
    private static final String USER = "root";
    private static final String PASSWORD = "mysql";

    private DefaultCategoryDataset dataset;

    public Bitmapindex(String title) {
        super(title);
        this.dataset = new DefaultCategoryDataset();
    }

    public void createDataset(String operation, long time) {
        dataset.addValue(time, operation, "Time (ms)");
    }

    public DefaultCategoryDataset getDataset() {
        return dataset;
    }

    public void createChart() {
        JFreeChart chart = ChartFactory.createBarChart("Operation Times", "Operation", "Time (ms)", getDataset(), PlotOrientation.VERTICAL, true, true, false);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
        setContentPane(chartPanel);
    }

    public static void main(String[] args) {
        Bitmapindex chart = new Bitmapindex("Operation Times");
        chart.pack();
        RefineryUtilities.centerFrameOnScreen(chart);
        chart.setVisible(true);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            Statement statement = connection.createStatement();

            createTable(statement);

            Map<String, Set<Integer>> bitmapIndex = new HashMap<>();

            long insertStartTime = System.currentTimeMillis();
            insertData(connection, bitmapIndex);
            long insertEndTime = System.currentTimeMillis();
            long insertionTime = insertEndTime - insertStartTime;

            displayBitmapIndex(bitmapIndex);

            long deleteStartTime = System.currentTimeMillis();
            deleteData(connection, bitmapIndex);
            long deleteEndTime = System.currentTimeMillis();
            long deletionTime = deleteEndTime - deleteStartTime;

            displayBitmapIndex(bitmapIndex);

            long queryStartTime = System.currentTimeMillis();
            Set<Integer> queryResult = searchData(bitmapIndex, "term_to_search");
            long queryEndTime = System.currentTimeMillis();
            long queryResponseTime = queryEndTime - queryStartTime;

            System.out.println("Query Result: " + queryResult);
            System.out.println("Insertion Time (ms): " + insertionTime);
            System.out.println("Deletion Time (ms): " + deletionTime);
            System.out.println("Query Response Time (ms): " + queryResponseTime);

            chart.createDataset("Insertion", insertionTime);
            chart.createDataset("Deletion", deletionTime);
            chart.createDataset("Query Response", queryResponseTime);

            statement.close();
            connection.close();

            chart.createChart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createTable(Statement statement) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS bitmap (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "name VARCHAR(255) NOT NULL," +
                "content TEXT NOT NULL)";
        statement.execute(createTableSQL);
    }

    private static void insertData(Connection connection, Map<String, Set<Integer>> bitmapIndex) throws SQLException {
        String[] sampleData = {
            "document1", "apple orange banana",
            "document2", "apple banana",
            "document3", "orange pineapple",
        };

        for (int i = 0; i < sampleData.length; i += 2) {
            String documentName = sampleData[i];
            String content = sampleData[i + 1];

            String insertSQL = "INSERT INTO bitmap (name, content) VALUES (?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, documentName);
            preparedStatement.setString(2, content);
            preparedStatement.executeUpdate();

            int id = getLastInsertedId(preparedStatement);
            updateBitmapIndex(bitmapIndex, content, id);
        }
    }

    private static int getLastInsertedId(PreparedStatement preparedStatement) throws SQLException {
        ResultSet resultSet = preparedStatement.getGeneratedKeys();
        if (resultSet.next()) {
            return resultSet.getInt(1);
        }
        return -1;
    }

    private static void updateBitmapIndex(Map<String, Set<Integer>> bitmapIndex, String content, int id) {
        for (String term : content.split("\\s+")) {
            term = term.toLowerCase();

            if (!bitmapIndex.containsKey(term)) {
                bitmapIndex.put(term, new HashSet<>());
            }

            bitmapIndex.get(term).add(id);
        }
    }

    private static void deleteData(Connection connection, Map<String, Set<Integer>> bitmapIndex) throws SQLException {
        String deleteSQL = "DELETE FROM bitmap WHERE name=?";
        PreparedStatement preparedStatement = connection.prepareStatement(deleteSQL);
        preparedStatement.setString(1, "document2");
        preparedStatement.executeUpdate();

        Set<Integer> deletedIds = Collections.singleton(2);
        updateBitmapIndexAfterDeletion(bitmapIndex, deletedIds);
    }

    private static void updateBitmapIndexAfterDeletion(Map<String, Set<Integer>> bitmapIndex, Set<Integer> deletedIds) {
        for (Map.Entry<String, Set<Integer>> entry : bitmapIndex.entrySet()) {
            entry.getValue().removeAll(deletedIds);
        }
    }

    private static Set<Integer> searchData(Map<String, Set<Integer>> bitmapIndex, String searchTerm) {
        searchTerm = searchTerm.toLowerCase();
        return bitmapIndex.getOrDefault(searchTerm, Collections.emptySet());
    }

    private static void displayBitmapIndex(Map<String, Set<Integer>> bitmapIndex) {
        for (Map.Entry<String, Set<Integer>> entry : bitmapIndex.entrySet()) {
            System.out.println("Term: " + entry.getKey() + ", Document IDs: " + entry.getValue());
        }
    }
}
