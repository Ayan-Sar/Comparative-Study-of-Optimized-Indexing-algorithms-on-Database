import java.sql.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

class BPlusTree extends ApplicationFrame {
    private Connection connection;
    private long totalInsertTime;
    private long totalDeleteTime;
    private long totalQueryTime;
    public BPlusTree(Connection connection) {
        super("B+ Tree Performance Metrics");
        this.connection = connection;
        createTableIfNotExists();
    }
    public void insert(String username, String email, String password, Timestamp lastLogin, boolean isActive, boolean isAdmin) throws SQLException {
        long startTime = System.nanoTime();
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO bplus (username, email, password, lastlogin, isactive, isadmin) VALUES (?, ?, ?, ?, ?, ?)")) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, email);
            preparedStatement.setString(3, password);
            preparedStatement.setTimestamp(4, lastLogin);
            preparedStatement.setBoolean(5, isActive);
            preparedStatement.setBoolean(6, isAdmin);
            preparedStatement.executeUpdate();
        }
        long endTime = System.nanoTime();
        totalInsertTime += (endTime - startTime);
    }
    public boolean delete(String username) throws SQLException {
        long startTime = System.nanoTime();
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM bplus WHERE username = ?")) {
            preparedStatement.setString(1, username);
            int rowsAffected = preparedStatement.executeUpdate();
            long endTime = System.nanoTime();
            totalDeleteTime += (endTime - startTime);
            return rowsAffected > 0;
        }
    }
    public boolean search(String username) throws SQLException {
        long startTime = System.nanoTime();
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM bplus WHERE username = ?")) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            int count = resultSet.getInt(1);
            long endTime = System.nanoTime();
            totalQueryTime += (endTime - startTime);
            return count > 0;
        }
    }
    private void createTableIfNotExists() {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS bplus (username VARCHAR(255) PRIMARY KEY, email VARCHAR(255), password VARCHAR(255), lastlogin DATETIME, isactive BOOLEAN, isadmin BOOLEAN)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void generatePerformanceGraphs() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(totalInsertTime, "Insert Time", "Total");
        dataset.addValue(totalDeleteTime, "Delete Time", "Total");
        dataset.addValue(totalQueryTime, "Query Time", "Total");
        JFreeChart chart = ChartFactory.createBarChart(
            "Performance Metrics",
            "Operation",
            "Time (nanoseconds)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
        setContentPane(chartPanel);
    }
    public static void main(String[] args) {
        String jdbcUrl = "jdbc:mysql://localhost:3306/minor";
        String username = "root";
        String password = "mysql";
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
            BPlusTree bPlusTree = new BPlusTree(connection);
            bPlusTree.insert("user1", "user1@example.com", "password1", new Timestamp(System.currentTimeMillis()), true, false);
            boolean exists = bPlusTree.search("user1");
            System.out.println("User exists: " + exists);
            boolean deleted = bPlusTree.delete("user1");
            System.out.println("User deleted: " + deleted);
            bPlusTree.generatePerformanceGraphs();
            bPlusTree.pack();
            RefineryUtilities.centerFrameOnScreen(bPlusTree);
            bPlusTree.setVisible(true);
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
