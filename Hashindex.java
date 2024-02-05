import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class Hashindex {
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/minor";
    static final String USER = "root";
    static final String PASS = "mysql";
    public static void main(String[] args) {
        Connection conn = null;
        PreparedStatement selectStatement = null;
        PreparedStatement insertStatement = null;
        PreparedStatement deleteStatement = null;
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            String selectSql = "SELECT h.id, h.keyhash, h.valuehash, hb.bucket FROM Hashes h " +
                    "INNER JOIN HashBuckets hb ON h.id = hb.hashId";
            selectStatement = conn.prepareStatement(selectSql);
            String insertSql = "INSERT INTO Hashes (keyhash, valuehash) VALUES (?, ?)";
            insertStatement = conn.prepareStatement(insertSql);
            String deleteSql = "DELETE FROM Hashes WHERE id = ?";
            deleteStatement = conn.prepareStatement(deleteSql);
            ResultSet rs = selectStatement.executeQuery();
            Map<Integer, Map<String, String>> hashIndex = new HashMap<>();
            while (rs.next()) {
                int id = rs.getInt("id");
                String key = rs.getString("keyhash");
                String value = rs.getString("valuehash");
                Map<String, String> keyValueMap = hashIndex.computeIfAbsent(id, k -> new HashMap<>());
                keyValueMap.put(key, value);
            }
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();           
            List<Long> insertionTimes = new ArrayList<>();
            List<Long> deletionTimes = new ArrayList<>();
            List<Long> queryTimes = new ArrayList<>();
            for (int i = 0; i < 5; i++) { 
                long startTime, endTime, elapsedTime;
                startTime = System.currentTimeMillis();
                insertData(insertStatement, "newKey" + i, "newValue" + i);
                endTime = System.currentTimeMillis();
                elapsedTime = endTime - startTime;
                insertionTimes.add(elapsedTime);
                int idToDelete = 1;
                startTime = System.currentTimeMillis();
                deleteData(deleteStatement, idToDelete);
                endTime = System.currentTimeMillis();
                elapsedTime = endTime - startTime;
                deletionTimes.add(elapsedTime);
                String keyToQuery = "existingKey";
                startTime = System.currentTimeMillis();
                queryData(hashIndex, keyToQuery);
                endTime = System.currentTimeMillis();
                elapsedTime = endTime - startTime;
                queryTimes.add(elapsedTime);
            }
            for (int i = 0; i < insertionTimes.size(); i++) {
                dataset.addValue(insertionTimes.get(i), "Insertion Time", "Iteration " + (i + 1));
                dataset.addValue(deletionTimes.get(i), "Deletion Time", "Iteration " + (i + 1));
                dataset.addValue(queryTimes.get(i), "Query Time", "Iteration " + (i + 1));
            }
            JFreeChart chart = ChartFactory.createBarChart(
                    "Performance Metrics", "Iteration", "Time (ms)",
                    dataset, PlotOrientation.VERTICAL, true, true, false);
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
            ApplicationFrame frame = new ApplicationFrame("Performance Metrics");
            frame.setContentPane(chartPanel);
            frame.pack();
            RefineryUtilities.centerFrameOnScreen(frame);
            frame.setVisible(true);
            rs.close();
            selectStatement.close();
            insertStatement.close();
            deleteStatement.close();
            conn.close();
        } catch (SQLException se) {
            se.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (selectStatement != null) selectStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (conn != null) conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    public static void insertData(PreparedStatement statement, String keyhash, String valuehash) throws SQLException {
        if (!isKeyExistsInTable(statement.getConnection(), keyhash)) {
            statement.setString(1, keyhash);
            statement.setString(2, valuehash);
            statement.executeUpdate();
        } else {
            System.out.println("Key already exists. You can choose to update or handle it differently.");
        }
    }
    public static boolean isKeyExistsInTable(Connection connection, String keyhash) throws SQLException {
        String query = "SELECT COUNT(*) FROM Hashes WHERE keyhash = ?";
        PreparedStatement checkStatement = connection.prepareStatement(query);
        checkStatement.setString(1, keyhash);
        ResultSet resultSet = checkStatement.executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        resultSet.close();
        checkStatement.close();
        return count > 0;
    }
    public static void deleteData(PreparedStatement statement, int id) throws SQLException {
        statement.setInt(1, id);
        statement.executeUpdate();
    }
    public static void queryData(Map<Integer, Map<String, String>> hashIndex, String key) {
        System.out.println("Query Results for Key: " + key);
        for (Map.Entry<Integer, Map<String, String>> entry : hashIndex.entrySet()) {
            int id = entry.getKey();
            Map<String, String> keyValueMap = entry.getValue();
            if (keyValueMap.containsKey(key)) {
                String value = keyValueMap.get(key);
                System.out.println("ID: " + id + "\tKEY: " + key + "\tVALUE: " + value);
            }
        }
    }
}
