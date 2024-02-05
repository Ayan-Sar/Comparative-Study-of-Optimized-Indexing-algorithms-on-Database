import java.awt.Dimension;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

class BTreeNode {
    int key;
    int value;
    BTreeNode left;
    BTreeNode right;
    public BTreeNode(int key, int value) {
        this.key = key;
        this.value = value;
        this.left = null;
        this.right = null;
    }
}
public class BTreeMySQL extends ApplicationFrame {
    private Connection connection;
    private DefaultCategoryDataset insertionTimeDataset;
    private DefaultCategoryDataset queryResponseTimeDataset;
    private DefaultCategoryDataset deletionEfficiencyDataset;
    public BTreeMySQL(String title) {
        super(title);
        insertionTimeDataset = new DefaultCategoryDataset();
        queryResponseTimeDataset = new DefaultCategoryDataset();
        deletionEfficiencyDataset = new DefaultCategoryDataset();
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/minor", "root", "mysql");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void insert(int id, int value) {
        long startTime = System.nanoTime();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO btree (id, value) VALUES (?, ?)");
            preparedStatement.setInt(1, id);
            preparedStatement.setInt(2, value);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        long endTime = System.nanoTime();
        long insertionTime = endTime - startTime;
        insertionTimeDataset.addValue(insertionTime, "Insertion Time", String.valueOf(id));
    }
    public int search(int id) {
        long startTime = System.nanoTime();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT value FROM btree WHERE id = ?");
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int result = resultSet.getInt("value");
                long endTime = System.nanoTime();
                long queryTime = endTime - startTime;
                queryResponseTimeDataset.addValue(queryTime, "Query Response Time", String.valueOf(id));
                return result;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    public void delete(int id) {
        long startTime = System.nanoTime();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM btree WHERE id = ?");
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        long endTime = System.nanoTime();
        long deletionTime = endTime - startTime;
        deletionEfficiencyDataset.addValue(deletionTime, "Deletion Efficiency", String.valueOf(id));
    }
    public void createCharts() {
        JFreeChart insertionTimeChart = ChartFactory.createLineChart(
                "Insertion Time", "ID", "Time (nanoseconds)", insertionTimeDataset, PlotOrientation.VERTICAL, true, true, false);
        JFreeChart queryResponseTimeChart = ChartFactory.createLineChart(
                "Query Response Time", "ID", "Time (nanoseconds)", queryResponseTimeDataset, PlotOrientation.VERTICAL, true, true, false);
        JFreeChart deletionEfficiencyChart = ChartFactory.createLineChart(
                "Deletion Efficiency", "ID", "Time (nanoseconds)", deletionEfficiencyDataset, PlotOrientation.VERTICAL, true, true, false);
        JPanel chartPanel = new JPanel();
        chartPanel.setLayout(new GridLayout(3, 1));
        ChartPanel insertionTimePanel = new ChartPanel(insertionTimeChart);
        ChartPanel queryResponseTimePanel = new ChartPanel(queryResponseTimeChart);
        ChartPanel deletionEfficiencyPanel = new ChartPanel(deletionEfficiencyChart);
        insertionTimePanel.setPreferredSize(new Dimension(800, 600));
        queryResponseTimePanel.setPreferredSize(new Dimension(800, 600));
        deletionEfficiencyPanel.setPreferredSize(new Dimension(800, 600));
        chartPanel.add(insertionTimePanel);
        chartPanel.add(queryResponseTimePanel);
        chartPanel.add(deletionEfficiencyPanel);
        setContentPane(chartPanel);
    }
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        BTreeMySQL bTree = new BTreeMySQL("B-Tree Metrics");
        for (int i = 1; i <= 1000; i++) {
            bTree.insert(i, i * 10);
        }
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            int key = random.nextInt(1000) + 1;
            int result = bTree.search(key);
            if (result != -1) {
                System.out.println("Value found: " + result);
            } else {
                System.out.println("Value not found.");
            }
        }
        for (int i = 1; i <= 10; i++) {
            bTree.delete(i);
        }
        bTree.createCharts();
        bTree.pack();
        RefineryUtilities.centerFrameOnScreen(bTree);
        bTree.setVisible(true);
    }
}