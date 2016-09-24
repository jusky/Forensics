package frame;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.TimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import model.*;
import tool.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tool.HTTPServer.addContentTypes;


public class MainFrame extends JFrame {
    private static final int WIDTH = 1560;
    private static final int HEIGHT = 1000;
    private static final int PORT = 1234;
    boolean flag = false;
    JPanel testPanel = new JPanel(new BorderLayout());
    JPanel infoPanel = new JPanel(new BorderLayout());
    JPanel menuPanel = new JPanel();
    final JTabbedPane tabbedPane = new JTabbedPane();
    final JTabbedPane tabbedPane2 = new JTabbedPane();
    ButtonGroup buttonGroup = new ButtonGroup();
    ImageIcon smsIcon = new ImageIcon("images/sms.jpg");
    ImageIcon wxIcon = new ImageIcon("images/weixin.jpg");
    ImageIcon qqIcon = new ImageIcon("images/qq.jpg");
    ImageIcon weiboIcon = new ImageIcon("images/weibo.jpg");
    ImageIcon mailIcon = new ImageIcon("images/mail.jpg");
    ImageIcon taobaoIcon = new ImageIcon("images/taobao.jpg");
    ImageIcon jdIcon = new ImageIcon("images/jd.jpg");
    ImageIcon ucIcon = new ImageIcon("images/ucbrowser.jpg");
    ImageIcon qqBrowserIcon = new ImageIcon("images/qqbrowser.jpg");
    ImageIcon xiechengIcon = new ImageIcon("images/xiecheng.jpg");
    ImageIcon callIcon = new ImageIcon("images/call.jpg");
    Logger logger = Logger.getLogger("MainFrame");
    HTTPServer server = null;
    String dbPath = null;
    List<SMS> smsList = new ArrayList<>();

    public MainFrame() {
        setFocusCycleRoot(true);
        setLayout(new BorderLayout(-10, 0));
        setTitle("取证精灵");
        setBounds(0, 30, 1400, 700);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        testPanel.setBackground(new Color(75, 10, 92));
        JLabel bgLabel = new JLabel();
        Icon backIcon = new ImageIcon("images/background.png");
        bgLabel.setIcon(backIcon);
        testPanel.add(bgLabel);
        add(testPanel);

        // 图片按钮菜单
        menuPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        menuPanel.setBackground(new Color(223, 223, 223));
        addButton("home", new HomeListener());
        addButton("import", new FileItemListener());
        addButton("browse", new BrowseListener());
        addButton("search", new SearchListener());
        addButton("recovery", new RecoveryListener());
        addButton("analyse", new AnalyseListener());
        addButton("map", new MapListener());
        addButton("reproduce", new ReproduceListener());
        addButton("evidence", null);
        addButton("report", null);
        addButton("help", null);
        add(menuPanel, BorderLayout.NORTH);
        for (int i = 2; i < 10; i++) {
            menuPanel.getComponent(i).setEnabled(false);
        }
        ((JRadioButton) menuPanel.getComponent(0)).doClick();

        // 开启服务器
        try {
            File dir = new File("WebRoot");
            if (!dir.canRead())
                throw new FileNotFoundException(dir.getAbsolutePath());
            // set up server
            for (File f : Arrays.asList(new File("/etc/mime.types"), new File(dir, ".mime.types")))
                if (f.exists())
                    addContentTypes(f);
            server = new HTTPServer(PORT);
            HTTPServer.VirtualHost host = server.getVirtualHost(null); // default host
            host.setAllowGeneratedIndex(true); // with directory index pages
            host.addContext("/", new HTTPServer.FileContextHandler(dir, "/"));
            host.addContext("/api/time", new HTTPServer.ContextHandler() {
                public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
                    long now = System.currentTimeMillis();
                    resp.getHeaders().add("Content-Type", "text/plain");
                    resp.send(200, String.format("%tF %<tT", now));
                    return 0;
                }
            });
            server.start();
            logger.info("HTTPServer正在监听端口" + PORT);

        } catch (Exception e) {
            logger.severe("error: " + e);
        }

        // 关闭窗口时关闭服务器
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                server.stop();
                System.exit(0);
            }
        });
    }

    private void addButton(String iconName, ActionListener listener) {
        JRadioButton button = new JRadioButton();
        button.setContentAreaFilled(false);
        button.setIcon(new ImageIcon("images/" + iconName + "_unselected.png"));
        button.setRolloverIcon(new ImageIcon("images/" + iconName + "_hover.png"));
        button.setSelectedIcon(new ImageIcon("images/" + iconName + "_selected.png"));
        button.setDisabledIcon(new ImageIcon("images/" + iconName + "_disabled.png"));
        button.addActionListener(listener);
        buttonGroup.add(button);
        menuPanel.add(button);
    }

    private class HomeListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.info("回到首页");
            testPanel.removeAll();
            flag = false;
            testPanel.setBackground(new Color(75, 10, 92));
            JLabel bgLabel = new JLabel(new ImageIcon("images/background.png"));
            testPanel.add(bgLabel);
            testPanel.revalidate();
            testPanel.repaint();
        }
    }

    private class FileItemListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.info("文件导入");
            flag = true;
            ImportFrame importFrame = new ImportFrame(MainFrame.this);
            importFrame.setVisible(true);
        }
    }

    private class BrowseListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.info("数据浏览");
            testPanel.removeAll();
            JPanel choosePanel = new JPanel();
            JLabel label = new JLabel("请选择手机：");
            choosePanel.add(label);
            Vector<String> phones = new Vector<>();
            phones.add("全部");
            ArrayList<String> phonePath = new ArrayList<>();
            Connection connection = null;
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT id, owner_name, phone_number, default_path FROM phones");
                while (rs.next()) {
                    int id = rs.getInt(1);
                    String ownerName = rs.getString(2);
                    String phoneNumber = rs.getString(3);
                    String path = rs.getString(4);
                    phonePath.add(path);
                    phones.add("手机" + id + " " + ownerName + " " + phoneNumber);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            JComboBox phoneCombo = new JComboBox(phones);
            choosePanel.add(phoneCombo);
            phoneCombo.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    try {
                        int index = phoneCombo.getSelectedIndex();
                        if (index != 0) {
                            logger.info(phonePath.get(index - 1));
                            logger.info(CallsFinder.findCalls(phonePath.get(index - 1) + "/contacts2.db").isEmpty()+"");
                            setData(SMSFinder.findSMS(phonePath.get(index - 1) + "/mmssms.db"),
                                    CallsFinder.findCalls(phonePath.get(index - 1) + "/contacts2.db"),
                                    EmailFinder.findEmail(phonePath.get(index - 1) + "/EmailProvider.db"),
                                    UCBrowserFinder.findUCBrowser(phonePath.get(index - 1) + "/history.db"),
                                    QQBrowserFinder.findQQBrowser(phonePath.get(index - 1) + "/database.db"),
                                    HotelFinder.findHotel(phonePath.get(index - 1) + "/ctrip_hotel.db"),
                                    FlightFinder.findFlight(phonePath.get(index - 1) + "/ctrip_flight.db")
                            );
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            phoneCombo.setSelectedIndex(phones.size() - 1);
            tabbedPane.setSelectedIndex(0);
            testPanel.add(choosePanel, BorderLayout.NORTH);
            testPanel.revalidate();
            testPanel.repaint();
        }
    }

    private class SearchListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.info("数据查询");
            testPanel.removeAll();
            JPanel searchPanel = new JPanel();
            JPanel detailPanel = new JPanel();
            JButton searchButton = new JButton("搜索");
            // JScrollPane scrollPane = new JScrollPane();
            GridBagLayout layout = new GridBagLayout();
            searchPanel.setLayout(layout);
            detailPanel.setLayout(layout);
            JLabel hintLabel = new JLabel("请选择查询数据类型：");
            searchPanel.add(hintLabel);

            JButton smsButton = new JButton("短信");
            //smsButton.setIcon(smsIcon);
            searchPanel.add(smsButton);
            smsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    detailPanel.removeAll();

                    JCheckBox contactCheckBox = new JCheckBox();
                    JLabel contactLabel = new JLabel("联系人：");
                    JTextField contactTextField = new JTextField(10);
                    detailPanel.add(contactCheckBox);
                    detailPanel.add(contactLabel);
                    detailPanel.add(contactTextField);

                    JCheckBox keywordCheckBox = new JCheckBox();
                    JLabel keywordLabel = new JLabel("关键字：");
                    JTextField keywordTextField = new JTextField(10);
                    detailPanel.add(keywordCheckBox);
                    detailPanel.add(keywordLabel);
                    detailPanel.add(keywordTextField);

                    JCheckBox startDateCheckBox = new JCheckBox();
                    JLabel startDateLabel = new JLabel("起始日期");
                    DatePickerSettings datePickerSettings = new DatePickerSettings();
                    datePickerSettings.setFormatForDatesCommonEra("yyyy-MM-dd");
                    DatePicker startDatePicker = new DatePicker(datePickerSettings);
                    detailPanel.add(startDateCheckBox);
                    detailPanel.add(startDateLabel);
                    detailPanel.add(startDatePicker);

                    JCheckBox endDateCheckBox = new JCheckBox();
                    JLabel endDateLabel = new JLabel("截止日期");
                    DatePicker endDatePicker = new DatePicker(datePickerSettings.copySettings());
                    detailPanel.add(endDateCheckBox);
                    detailPanel.add(endDateLabel);
                    detailPanel.add(endDatePicker);

                    JCheckBox startTimeCheckBox = new JCheckBox();
                    JLabel startTimeLabel = new JLabel("开始时间");
                    TimePickerSettings settings = new TimePickerSettings();
                    settings.setDisplaySpinnerButtons(true);
                    settings.setFormatForDisplayTime("HH:mm");
                    TimePicker startTimePicker = new TimePicker(settings);
                    detailPanel.add(startTimeCheckBox);
                    detailPanel.add(startTimeLabel);
                    detailPanel.add(startTimePicker);

                    JCheckBox endTimeCheckBox = new JCheckBox();
                    JLabel endTimeLabel = new JLabel("截止时间");
                    TimePicker endTimePicker = new TimePicker(settings);
                    detailPanel.add(endTimeCheckBox);
                    detailPanel.add(endTimeLabel);
                    detailPanel.add(endTimePicker);

                    detailPanel.add(searchButton);

                    GridBagConstraints constraints = new GridBagConstraints();
                    constraints.weightx = 1;
                    constraints.gridwidth = 0;
                    constraints.fill = GridBagConstraints.HORIZONTAL;
                    layout.setConstraints(contactTextField, constraints);
                    layout.setConstraints(keywordTextField, constraints);
                    layout.setConstraints(startDatePicker, constraints);
                    layout.setConstraints(endDatePicker, constraints);
                    layout.setConstraints(startTimePicker, constraints);
                    layout.setConstraints(endTimePicker, constraints);
                    layout.setConstraints(searchButton, constraints);

                    searchButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            System.out.println("Start search");
                            String[] columnNames = {"手机", "联系人", "时间", "通话长度", "类型"};
                            String[][] data = null;
                            DefaultTableModel model = new DefaultTableModel(data, columnNames) {
                                @Override
                                public boolean isCellEditable(int row, int column) {
                                    return false;
                                }
                            };
                            JTable table = new JTable(model);
                            StringBuffer sql = new StringBuffer("SELECT '手机' || messages.phone_id || \" \" || phones.phone_number AS phone," +
                                    "messages.number || ' ' || ifnull(contacts.name, '') AS num," +
                                    "messages.body AS body, messages.date AS date, messages.status AS type" +
                                    "  FROM messages LEFT OUTER JOIN contacts" +
                                    " ON messages.number = contacts.number AND messages.phone_id = contacts.phone_id, phones" +
                                    " WHERE phones.id = messages.phone_id ");
                            if (!contactTextField.getText().isEmpty() && contactCheckBox.isSelected()) {
                                sql.append("AND num LIKE '%" + contactTextField.getText() + "%'");
                            }
                            if (!keywordTextField.getText().isEmpty() && keywordCheckBox.isSelected()) {
                                sql.append(" AND body LIKE '%" + keywordTextField.getText() + "%'");
                            }
                            if (!startDatePicker.getText().isEmpty() && startDateCheckBox.isSelected()) {
                                sql.append(" AND date >= '" + startDatePicker.getText() + " 00:00:00'");
                            }
                            if (!endDatePicker.getText().isEmpty() && endDateCheckBox.isSelected()) {
                                sql.append(" AND date <= '" + endDatePicker.getText() + " 23:59:59'");
                            }
                            sql.append(";");
                            Connection connection = null;
                            try {
                                connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                                Statement statement = connection.createStatement();

                                ResultSet rs = statement.executeQuery(sql.toString());
                                while (rs.next()) {
                                    String[] row = new String[5];
                                    for (int i = 0; i < 4; i++)
                                        row[i] = rs.getString(i + 1);
                                    switch (rs.getInt(5)) {
                                        case 1:
                                            row[4] = "接到";
                                            break;
                                        case 2:
                                            row[4] = "发出";
                                            break;
                                        default:
                                            row[4] = "";
                                    }
                                    model.addRow(row);
                                    System.out.println(Arrays.toString(row));
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            testPanel.removeAll();
                            testPanel.add(searchPanel, BorderLayout.WEST);
                            JScrollPane scrollPane = new JScrollPane(table);
                            testPanel.add(scrollPane);
                            testPanel.revalidate();
                            testPanel.repaint();
                        }
                    });

                    detailPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "短信查询"));
                    detailPanel.revalidate();
                    detailPanel.repaint();
                }
            });

            JButton callButton = new JButton("通话记录");
            //callButton.setIcon(callIcon);
            searchPanel.add(callButton);
            callButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    detailPanel.removeAll();

                    JCheckBox contactCheckBox = new JCheckBox();
                    JLabel contactLabel = new JLabel("联系人：");
                    JTextField contactTextField = new JTextField(10);
                    detailPanel.add(contactCheckBox);
                    detailPanel.add(contactLabel);
                    detailPanel.add(contactTextField);

                    JCheckBox startDateCheckBox = new JCheckBox();
                    JLabel startDateLabel = new JLabel("起始日期");
                    DatePickerSettings datePickerSettings = new DatePickerSettings();
                    datePickerSettings.setFormatForDatesCommonEra("yyyy-MM-dd");
                    DatePicker startDatePicker = new DatePicker(datePickerSettings);
                    detailPanel.add(startDateCheckBox);
                    detailPanel.add(startDateLabel);
                    detailPanel.add(startDatePicker);

                    JCheckBox endDateCheckBox = new JCheckBox();
                    JLabel endDateLabel = new JLabel("截止日期");
                    DatePicker endDatePicker = new DatePicker(datePickerSettings.copySettings());
                    detailPanel.add(endDateCheckBox);
                    detailPanel.add(endDateLabel);
                    detailPanel.add(endDatePicker);

                    JCheckBox startTimeCheckBox = new JCheckBox();
                    JLabel startTimeLabel = new JLabel("开始时间");
                    TimePickerSettings settings = new TimePickerSettings();
                    settings.setDisplaySpinnerButtons(true);
                    settings.setFormatForDisplayTime("HH:mm");
                    TimePicker startTimePicker = new TimePicker(settings);
                    detailPanel.add(startTimeCheckBox);
                    detailPanel.add(startTimeLabel);
                    detailPanel.add(startTimePicker);

                    JCheckBox endTimeCheckBox = new JCheckBox();
                    JLabel endTimeLabel = new JLabel("截止时间");
                    TimePicker endTimePicker = new TimePicker(settings);
                    detailPanel.add(endTimeCheckBox);
                    detailPanel.add(endTimeLabel);
                    detailPanel.add(endTimePicker);

                    detailPanel.add(searchButton);

                    GridBagConstraints constraints = new GridBagConstraints();
                    constraints.weightx = 1;
                    constraints.gridwidth = 0;
                    constraints.fill = GridBagConstraints.HORIZONTAL;
                    layout.setConstraints(contactTextField, constraints);
                    layout.setConstraints(startDatePicker, constraints);
                    layout.setConstraints(endDatePicker, constraints);
                    layout.setConstraints(startTimePicker, constraints);
                    layout.setConstraints(endTimePicker, constraints);
                    layout.setConstraints(searchButton, constraints);

                    searchButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            System.out.println("Start search");
                            String[] columnNames = {"手机", "联系人", "时间", "通话长度", "类型"};
                            String[][] data = null;
                            DefaultTableModel model = new DefaultTableModel(data, columnNames) {
                                @Override
                                public boolean isCellEditable(int row, int column) {
                                    return false;
                                }
                            };
                            JTable table = new JTable(model);
                            StringBuffer sql = new StringBuffer("SELECT '手机' || calls.phone_id || \" \" || phones.phone_number AS phone," +
                                    "calls.number || ' ' || ifnull(contacts.name, '') AS num," +
                                    "calls.date AS date, calls.duration AS duration, calls.type AS type" +
                                    "  FROM calls LEFT OUTER JOIN contacts" +
                                    " ON calls.number = contacts.number AND calls.phone_id = contacts.phone_id, phones" +
                                    " WHERE phones.id = calls.phone_id ");
                            if (!contactTextField.getText().isEmpty() && contactCheckBox.isSelected()) {
                                sql.append("AND num LIKE '%" + contactTextField.getText() + "%'");
                            }
                            if (!startDatePicker.getText().isEmpty() && startDateCheckBox.isSelected()) {
                                sql.append(" AND date >= '" + startDatePicker.getText() + " 00:00:00'");
                            }
                            if (!endDatePicker.getText().isEmpty() && endDateCheckBox.isSelected()) {
                                sql.append(" AND date <= '" + endDatePicker.getText() + " 23:59:59'");
                            }
                            sql.append(";");
                            Connection connection = null;
                            try {
                                connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                                Statement statement = connection.createStatement();

                                ResultSet rs = statement.executeQuery(sql.toString());
                                while (rs.next()) {
                                    String[] row = new String[5];
                                    for (int i = 0; i < 4; i++)
                                        row[i] = rs.getString(i + 1);
                                    switch (rs.getInt(5)) {
                                        case 1:
                                            row[4] = "拨出";
                                            break;
                                        case 2:
                                            row[4] = "接听";
                                            break;
                                        case 3:
                                            row[4] = "未接";
                                            break;
                                        default:
                                            row[4] = "";
                                    }
                                    model.addRow(row);
                                    System.out.println(Arrays.toString(row));
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            testPanel.removeAll();
                            testPanel.add(searchPanel, BorderLayout.WEST);
                            JScrollPane scrollPane = new JScrollPane(table);
                            testPanel.add(scrollPane);
                            testPanel.revalidate();
                            testPanel.repaint();
                        }
                    });
                    detailPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "通话记录查询"));
                    detailPanel.revalidate();
                    detailPanel.repaint();
                }
            });

            JButton ucButton = new JButton("UC浏览器记录");
            //ucButton.setIcon(ucIcon);
            searchPanel.add(ucButton);
            ucButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    detailPanel.removeAll();
                    JCheckBox websiteCheckBox = new JCheckBox();
                    JLabel websiteLabel = new JLabel("网址：  ");
                    JTextField websiteTextField = new JTextField(10);
                    detailPanel.add(websiteCheckBox);
                    detailPanel.add(websiteLabel);
                    detailPanel.add(websiteTextField);

                    JCheckBox titleCheckBox = new JCheckBox();
                    JLabel titleLabel = new JLabel("网址标题");
                    JTextField titleTextField = new JTextField(10);
                    detailPanel.add(titleCheckBox);
                    detailPanel.add(titleLabel);
                    detailPanel.add(titleTextField);

                    detailPanel.add(searchButton);

                    GridBagConstraints constraints = new GridBagConstraints();
                    constraints.weightx = 1;
                    constraints.gridwidth = 0;
                    constraints.fill = GridBagConstraints.HORIZONTAL;
                    layout.setConstraints(websiteTextField, constraints);
                    layout.setConstraints(titleTextField, constraints);
                    layout.setConstraints(searchButton, constraints);
                    detailPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "搜索UC浏览器记录"));
                    detailPanel.revalidate();
                    detailPanel.repaint();
                }
            });

            JButton qbButton = new JButton("QQ浏览器记录");
            //qbButton.setIcon(qqBrowserIcon);
            searchPanel.add(qbButton);

            JButton mailButton = new JButton("邮件记录");
            //mailButton.setIcon(mailIcon);
            searchPanel.add(mailButton);

            JButton xcHotelButton = new JButton("携程酒店记录");
            //xcHotelButton.setIcon(xiechengIcon);
            searchPanel.add(xcHotelButton);

            JButton xcTicketButton = new JButton("携程机票记录");
            //xcTicketButton.setIcon(xiechengIcon);
            searchPanel.add(xcTicketButton);

            JButton wechatButton = new JButton("微信记录");
            //wechatButton.setIcon(wxIcon);
            searchPanel.add(wechatButton);

            JButton qqButton = new JButton("QQ记录");
            //qqButton.setIcon(qqIcon);
            searchPanel.add(qqButton);

            JLabel fillLabel = new JLabel();
            searchPanel.add(fillLabel);


            searchPanel.add(detailPanel);
            GridBagConstraints s = new GridBagConstraints();
            s.fill = GridBagConstraints.HORIZONTAL;
            s.insets = new Insets(1, 5, 0, 5);
            s.gridwidth = 0;
            s.weightx = 1;
            layout.setConstraints(hintLabel, s);
            s.gridwidth = 1;
            layout.setConstraints(smsButton, s);
            layout.setConstraints(ucButton, s);
            layout.setConstraints(mailButton, s);
            layout.setConstraints(xcTicketButton, s);
            s.gridwidth = 0;
            layout.setConstraints(xcHotelButton, s);
            layout.setConstraints(wechatButton, s);
            layout.setConstraints(callButton, s);
            layout.setConstraints(qbButton, s);
            s.weightx = 0.5;
            layout.setConstraints(qqButton, s);
            s.fill = GridBagConstraints.BOTH;
            s.weighty = 1;
            layout.setConstraints(fillLabel, s);
            layout.setConstraints(detailPanel, s);

            searchPanel.setPreferredSize(new Dimension(300, 500));
            testPanel.add(searchPanel, BorderLayout.WEST);
            testPanel.revalidate();
            testPanel.repaint();
        }
    }

    private class RecoveryListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.info("数据恢复");
            testPanel.removeAll();
            testPanel.setLayout(null);

            final JButton fileButton = new JButton("浏览");
            final JButton submitButton = new JButton("恢复数据库");

            fileButton.setBounds(800, 200, 70, 30);
            submitButton.setBounds(880, 200, 100, 30);
            final JTextArea t1 = new JTextArea(10, 50);
            t1.setLineWrap(true);// 激活自动换行功能
            t1.setWrapStyleWord(true);// 激活断行不断字功能
            t1.setBounds(10, 40, 10, 50);
            final JTextArea t2 = new JTextArea(10, 50);
            t2.setLineWrap(true);
            t2.setWrapStyleWord(true);
            t2.setBounds(60, 10, 10, 50);
            t2.append("整理：" + "\n\r");
            final JTextField fileTextField = new JTextField(60);
            fileTextField.setBounds(400, 200, 380, 30);
            fileButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {

                    JFileChooser fileChooser = new JFileChooser();
                    //fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);// 只能选择目录
                    String path = null;
                    java.io.File f = null;
                    int flag = 0;
                    try {
                        flag = fileChooser.showOpenDialog(null);
                    } catch (HeadlessException head) {
                        System.out.println("Open File Dialog ERROR!");
                    }

                    if (flag == JFileChooser.APPROVE_OPTION) {
                        // 获得该文件
                        f = fileChooser.getSelectedFile();
                        path = f.getPath();

                        fileTextField.setText(path);
                    }

                }
            });
            submitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.out.println("数据恢复过程");
                    fileButton.setBounds(800, 25, 70, 30);
                    submitButton.setBounds(880, 25, 100, 30);
                    fileTextField.setBounds(400, 25, 380, 30);

                    Runtime r = Runtime.getRuntime();
                    Process p = null;

                    try {
                        System.out.println("Starting the process..");
                        String pythonString = new String();
                        pythonString = "D:\\桌面\\python\\sqlparse.py -f " + fileTextField.getText() + " -o D:\\桌面\\python\\output.txt ";
                        System.out.println(pythonString);
                        p = r.exec("cmd.exe /c start python " + pythonString);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    FileReader fr;
                    String s = new String();
                    String s1 = new String();
                    String s2 = new String();
                    List<SMS> list = new ArrayList<SMS>();
                    try {
                        InputStreamReader isr = new InputStreamReader(new FileInputStream("D:\\桌面\\python\\output.txt"), "UTF-8");
                        BufferedReader br = new BufferedReader(isr);
                        while (br.readLine() != null) {
                            s = br.readLine();
                            s2 = checkNum(s);
                            t1.append(s + "\r\n");
                            if (!s2.isEmpty()) {
                                SMS sms = new SMS();
                                sms.setAddress(s2);
                                s1 = s;
                                s1 = s1.replaceAll(s2, "");
                                s1 = s1.replaceAll("[^\u0020-\u9FA5]", "");
                                sms.setBody(s1);

                                list.add(sms);
                                System.out.println(s2);
                                System.out.println(s);
                                for (int i = 0; i < s.length(); i++) {
                                    if ((s.charAt(i) + "").getBytes().length > 1) {
                                        t2.append(s.charAt(i) + "");
                                    }
                                }
                                t2.append("\n\r");
                            }


                        }
                        br.close();
                        String[] columnNames = {"联系人", "短信内容", "数据状态"}; // 定义表格列名数组
                        // 定义表格数据数组
                        String[][] tableValues = new String[list.size() + 1][5];
                        for (int i = 0; i < list.size(); i++) {
                            tableValues[i][0] = list.get(i).getAddress();
                            tableValues[i][1] = list.get(i).getBody();
                            //if (smsList.get(i).getType() == 1)
                            //	tableValues[i][3] = "接收";
                            //if (smsList.get(i).getType() == 2)
                            //	tableValues[i][3] = "发送";
                            tableValues[i][2] = "已删除数据";
                        }
                        JTable smsTable = new JTable(tableValues, columnNames) {
                            private static final long serialVersionUID = 1L;
                        };
                        JScrollPane scrollPane = new JScrollPane(smsTable);

                        tabbedPane2.addTab("短信", new ImageIcon("images/sms.jpg"), scrollPane, "短信");
                        tabbedPane2.setSelectedIndex(0);

                    } catch (FileNotFoundException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }


                    JPanel test2Pane = new JPanel(new GridLayout(1, 3));
                    test2Pane.setBounds(30, 70, 1290, 440);
                    test2Pane.add(tabbedPane2);
                    test2Pane.add(t1);
                    test2Pane.add(t2);
                    testPanel.add(test2Pane);

                }

            });


            //testPane.add(tabbedPane2);
            testPanel.add(fileTextField);
            testPanel.add(fileButton);
            testPanel.add(submitButton);

            testPanel.repaint();
            testPanel.doLayout();

        }
    }

    private class ReproduceListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.info("情景重现");
            testPanel.removeAll();
            Runtime r = Runtime.getRuntime();
            Process p = null;
            try {
                System.out.println("Starting the process..");
                p = r.exec("cmd.exe /c start emulator -avd ForensicsElf");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            testPanel.setLayout(null);
            final JLabel appLabel = new JLabel("请选择装载的app：");
            appLabel.setFont(new Font("宋体", Font.PLAIN, 20));
            appLabel.setForeground(Color.WHITE);
            appLabel.setBounds(300, 170, 200, 30);

            final JButton fileButton = new JButton("浏览");
            final JButton submitButton = new JButton("装载");

            fileButton.setBounds(800, 200, 70, 30);
            submitButton.setBounds(880, 200, 70, 30);
            final JLabel dataLabel = new JLabel("请选择导入的用户数据：");
            dataLabel.setFont(new Font("宋体", Font.PLAIN, 20));
            dataLabel.setForeground(Color.WHITE);
            dataLabel.setBounds(300, 300, 250, 30);
            final JButton fileButton2 = new JButton("浏览");
            final JButton submitButton2 = new JButton("导入");

            fileButton2.setBounds(800, 330, 70, 30);
            submitButton2.setBounds(880, 330, 70, 30);
            final JTextField fileTextField = new JTextField(60);
            fileTextField.setBounds(400, 200, 380, 30);
            final JTextField fileTextField2 = new JTextField(60);
            fileTextField2.setBounds(400, 330, 380, 30);

            final JButton removeButton = new JButton("重新选择");
            removeButton.setBounds(960, 200, 70, 30);
            final JButton removeButton2 = new JButton("重新选择");
            removeButton2.setBounds(960, 330, 70, 30);

            fileButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {

                    JFileChooser fileChooser = new JFileChooser();
                    //fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);// 只能选择目录
                    String path = null;
                    java.io.File f = null;
                    int flag = 0;
                    try {
                        flag = fileChooser.showOpenDialog(null);
                    } catch (HeadlessException head) {
                        System.out.println("Open File Dialog ERROR!");
                    }

                    if (flag == JFileChooser.APPROVE_OPTION) {
                        // 获得该文件
                        f = fileChooser.getSelectedFile();
                        path = f.getPath();

                        fileTextField.setText(path);
                    }

                }
            });

            submitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Runtime r1 = Runtime.getRuntime();
                    Process p1 = null;
                    try {
                        System.out.println("Starting the process..");
                        p1 = r1.exec("cmd.exe /c start adb install -r " + fileTextField.getText());
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            });
            fileButton2.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {

                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);// 只能选择目录
                    String path = null;
                    java.io.File f = null;
                    int flag = 0;
                    try {
                        flag = fileChooser.showOpenDialog(null);
                    } catch (HeadlessException head) {
                        System.out.println("Open File Dialog ERROR!");
                    }

                    if (flag == JFileChooser.APPROVE_OPTION) {
                        // 获得该文件
                        f = fileChooser.getSelectedFile();
                        path = f.getPath();

                        fileTextField2.setText(path);
                    }

                }
            });

            submitButton2.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Runtime r1 = Runtime.getRuntime();
                    Process p1 = null;
                    try {
                        System.out.println("Starting the process..");
                        p1 = r1.exec("cmd.exe /c start adb push " + fileTextField2.getText() + " /data/data/com.baidu.tieba");
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            });
            removeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fileTextField.setText("");
                }
            });
            removeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fileTextField2.setText("");
                }
            });
            testPanel.add(appLabel);
            testPanel.add(dataLabel);
            testPanel.add(fileButton);
            testPanel.add(submitButton);
            testPanel.add(removeButton);
            testPanel.add(fileButton2);
            testPanel.add(submitButton2);
            testPanel.add(removeButton2);
            testPanel.add(fileTextField);
            testPanel.add(fileTextField2);
            testPanel.repaint();
            testPanel.doLayout();
        }
    }

    private class AnalyseListener implements ActionListener {
        JMenuBar menuBar = new JMenuBar();
        JFXPanel webBrowser = new JFXPanel();
        ImageIcon singleIcon1 = new ImageIcon("images/single-1.png");
        ImageIcon singleIcon2 = new ImageIcon("images/single-2.png");
        ImageIcon singleIcon3 = new ImageIcon("images/single-2.png");
        ImageIcon multiIcon1 = new ImageIcon("images/multi-1.png");
        ImageIcon multiIcon2 = new ImageIcon("images/multi-2.png");
        ImageIcon multiIcon3 = new ImageIcon("images/multi-2.png");
        public void actionPerformed(ActionEvent e) {
            System.out.println("数据分析界面");
            testPanel.removeAll();
            testPanel.setLayout(null);
            testPanel.setBackground(new Color(245, 245, 245));

            JRadioButton singleRadio = new JRadioButton(singleIcon1);
            singleRadio.setContentAreaFilled(false);
            singleRadio.setRolloverIcon(singleIcon2);
            singleRadio.setSelectedIcon(singleIcon3);
            singleRadio.addActionListener(new SingleListener());
            singleRadio.setBounds(350, 210, 200, 200);

            JRadioButton multiRadio = new JRadioButton(multiIcon1);
            multiRadio.setContentAreaFilled(false);
            multiRadio.setRolloverIcon(multiIcon2);
            multiRadio.setSelectedIcon(multiIcon3);
            multiRadio.addActionListener(new MultiListener());
            multiRadio.setBounds(800, 210, 200, 200);

            testPanel.add(singleRadio);
            testPanel.add(multiRadio);

            testPanel.revalidate();
            testPanel.repaint();


        }
        class SingleListener implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                try {

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                testPanel.add(menuBar, BorderLayout.WEST);
                testPanel.revalidate();
                testPanel.repaint();
            }
        }
        class MultiListener implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                System.out.println("run " + e.getActionCommand());
                if(true) {
                    System.out.println("ok");
                    testPanel.removeAll();
                    testPanel.add(webBrowser);
                    webBrowser.setBounds(0, 0, 1600, 1200);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Group root = new Group();
                            Scene scene = new Scene(root, WIDTH, HEIGHT);
                            webBrowser.setScene(scene);
                            Double widthDouble = new Integer(1500).doubleValue();
                            Double heightDouble = new Integer(800).doubleValue();

                            VBox box = new VBox(10);
                            HBox urlBox = new HBox(10);
                            WebView view = new WebView();
                            view.setMinSize(widthDouble, heightDouble);
                            view.setPrefSize(widthDouble, heightDouble);
                            final WebEngine eng = view.getEngine();
                            System.out.println("ttttt");
                            eng.load("http://localhost:1234/1.html");
                            root.getChildren().add(view);
                            box.getChildren().add(urlBox);
                            box.getChildren().add(view);
                            root.getChildren().add(box);
                            System.out.println("t");

                        }
                    });

                    testPanel.repaint();
                    testPanel.doLayout();
                }
            }
        }
    }

    private class MapListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.info("轨迹图");
            testPanel.removeAll();
            JFXPanel webBrowser = new JFXPanel();
            WebEngine engine;
            JPanel panel = new JPanel(new BorderLayout());
            JTextField imageUrl = new JTextField(80);
            JButton chooseButton = new JButton("选择文件夹");
            JButton goButton = new JButton("开始分析");

            JPanel topBar = new JPanel();
            topBar.add(imageUrl);
            topBar.add(chooseButton);
            topBar.add(goButton);
            testPanel.add(topBar, BorderLayout.NORTH);
            testPanel.add(webBrowser, BorderLayout.CENTER);

            chooseButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    File f = null;
                    try {
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            f = chooser.getSelectedFile();
                            imageUrl.setText(f.getPath());
                        }
                    } catch (HeadlessException ex) {
                        ex.printStackTrace();
                    }
                }
            });

            goButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                PhotoAnalyzer.write(imageUrl.getText());
                                WebView view = new WebView();
                                logger.info("Loading Track HTML...");
                                view.getEngine().load("WebRoot/track.html");
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            });
            testPanel.add(webBrowser);
            testPanel.revalidate();
            testPanel.repaint();
        }
    }

    public void setData(List<SMS> SMSList, List<Call> callsList, List<Email> emailList, List<Browser> UCBrowser,
                        List<Browser> QQBrowser, List<Hotel> hotelList, List<Flight> flightList) {
        this.smsList = SMSList;
        testPanel.add(infoPanel);
        infoPanel.setLayout(new BorderLayout());
        infoPanel.setBackground(new Color(223, 223, 223));
        // 设置选项卡标签的布局方式
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                // 获得被选中选项卡的索引
                int selectedIndex = tabbedPane.getSelectedIndex();
                // 获得指定索引的选项卡标签
                String title = tabbedPane.getTitleAt(selectedIndex);
                System.out.println(title);
            }
        });

        infoPanel.add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.removeAll();
        if (!SMSList.isEmpty())
            updatesms(SMSList);
        if (!callsList.isEmpty())
            updatecalls(callsList);
        if (!UCBrowser.isEmpty())
            updateUCBrowser(UCBrowser);
        if (!QQBrowser.isEmpty())
            updateQQBrowser(QQBrowser);
        if (!emailList.isEmpty())
            updateEmail(emailList);
        if (!hotelList.isEmpty())
            updateHotel(hotelList);
        if (!flightList.isEmpty())
            updateFlight(flightList);

        testPanel.revalidate();
        testPanel.repaint();
        // update(SMSList,callsList);
    }

    private static String checkNum(String num) {
        if (num == null || num.length() == 0) {
            return "";
        }
        Pattern pattern = Pattern.compile("(?<!\\d)(?:(?:1[358]\\d{9})|(?:861[358]\\d{9}))(?!\\d)");
        Matcher matcher = pattern.matcher(num);
        StringBuffer bf = new StringBuffer(64);
        while (matcher.find()) {
            bf.append(matcher.group()).append(",");
        }
        int len = bf.length();
        if (len > 0) {
            bf.deleteCharAt(len - 1);
        }
        return bf.toString();
    }

    public void updatesms(List<SMS> smsList) {
        String[] columnNames = {"时间", "联系人", "短信内容", "呼叫类型", "数据状态"}; // 定义表格列名数组
        // 定义表格数据数组
        String[][] tableValues = new String[smsList.size() + 1][5];
        for (int i = 0; i < smsList.size(); i++) {
            tableValues[i][0] = smsList.get(i).getTime();
            tableValues[i][1] = smsList.get(i).getAddress();
            tableValues[i][2] = smsList.get(i).getBody();
            if (smsList.get(i).getType() == 1)
                tableValues[i][3] = "接收";
            if (smsList.get(i).getType() == 2)
                tableValues[i][3] = "发送";
            tableValues[i][4] = "未删除数据";
        }
        JTable smsTable = new JTable(tableValues, columnNames) {
            private static final long serialVersionUID = 1L;
        };
        JScrollPane scrollPane = new JScrollPane(smsTable);
        tabbedPane.addTab("短信", smsIcon, scrollPane, "短信");
        tabbedPane.setSelectedIndex(0);
    }

    public void updatecalls(List<Call> callsList) {
        String[] columnNames = {"联系时间", "联系人", "持续时间", "呼叫状态", "数据状态"}; // 定义表格列名数组
        // 定义表格数据数组
        String[][] tableValues = new String[callsList.size() + 1][5];
        for (int i = 0; i < callsList.size(); i++) {
            tableValues[i][0] = callsList.get(i).getTime();
            tableValues[i][1] = callsList.get(i).getNumber();
            tableValues[i][2] = "" + callsList.get(i).getDuration();
            if (callsList.get(i).getType() == 1)
                tableValues[i][3] = "拨出";
            if (callsList.get(i).getType() == 2)
                tableValues[i][3] = "接听";
            tableValues[i][4] = "未删除数据";
        }
        JTable callsTable = new JTable(tableValues, columnNames) {
            private static final long serialVersionUID = 1L;
        };
        // 创建显示表格的滚动面板
        JScrollPane scrollPane2 = new JScrollPane(callsTable);
        tabbedPane.addTab("通话记录", callIcon, scrollPane2, "通话记录");
    }

    public void updateUCBrowser(List<Browser> ucBrowserList) {
        String[] columnNames = {"访问时间", "网站标题", "URL", "数据来源", "数据状态"}; // 定义表格列名数组
        // 定义表格数据数组
        String[][] tableValues = new String[ucBrowserList.size()][5];
        for (int i = 0; i < ucBrowserList.size(); i++) {
            tableValues[i][0] = ucBrowserList.get(i).getTime();
            tableValues[i][1] = ucBrowserList.get(i).getName();
            tableValues[i][2] = ucBrowserList.get(i).getUrl();
            tableValues[i][3] = ucBrowserList.get(i).getResource();
            tableValues[i][4] = "未删除数据";
        }
        JTable browserTable = new JTable(tableValues, columnNames) {
            private static final long serialVersionUID = 1L;
        };
        // 创建显示表格的滚动面板
        JScrollPane scrollPane3 = new JScrollPane(browserTable);
        tabbedPane.addTab("UC浏览器记录", ucIcon, scrollPane3, "UC浏览器记录");
    }

    public void updateQQBrowser(List<Browser> qqBrowserList) {
        String[] columnNames = {"访问时间", "网站标题", "URL", "数据来源", "数据状态"}; // 定义表格列名数组
        // 定义表格数据数组
        String[][] tableValues = new String[qqBrowserList.size()][5];
        for (int i = 0; i < qqBrowserList.size(); i++) {
            tableValues[i][0] = qqBrowserList.get(i).getTime();
            tableValues[i][1] = qqBrowserList.get(i).getName();
            tableValues[i][2] = qqBrowserList.get(i).getUrl();
            tableValues[i][3] = qqBrowserList.get(i).getResource();
            tableValues[i][4] = "未删除数据";
        }
        JTable browserTable = new JTable(tableValues, columnNames) {
            private static final long serialVersionUID = 1L;
        };
        // 创建显示表格的滚动面板
        JScrollPane scrollPane4 = new JScrollPane(browserTable);
        tabbedPane.addTab("QQ浏览器记录", qqBrowserIcon, scrollPane4, "QQ浏览器记录");
    }

    public void updateEmail(List<Email> emailList) {
        String[] columnNames = {"收发时间", "邮件标题", "发送人", "接收人", "数据状态"}; // 定义表格列名数组
        // 定义表格数据数组
        String[][] tableValues = new String[emailList.size()][5];
        for (int i = 0; i < emailList.size(); i++) {
            tableValues[i][0] = emailList.get(i).getTime();
            tableValues[i][1] = emailList.get(i).getSubject();
            tableValues[i][2] = emailList.get(i).getFromList();
            tableValues[i][3] = emailList.get(i).getToList();
            tableValues[i][4] = "未删除数据";
        }
        JTable browserTable = new JTable(tableValues, columnNames) {
            private static final long serialVersionUID = 1L;
        };
        // 创建显示表格的滚动面板
        JScrollPane scrollPane5 = new JScrollPane(browserTable);
        tabbedPane.addTab("邮件记录", mailIcon, scrollPane5, "邮件记录");
    }

    public void updateHotel(List<Hotel> hotelList) {
        String[] columnNames = {"查询时间", "城市名称", "位置坐标", "具体地址", "数据状态"}; // 定义表格列名数组
        // 定义表格数据数组
        String[][] tableValues = new String[hotelList.size()][5];
        for (int i = 0; i < hotelList.size(); i++) {
            tableValues[i][0] = hotelList.get(i).getTime();
            tableValues[i][1] = hotelList.get(i).getCityName();
            tableValues[i][2] = hotelList.get(i).getPositionCoordinates();
            tableValues[i][3] = hotelList.get(i).getPositionName();
            tableValues[i][4] = "未删除数据";
        }
        JTable browserTable = new JTable(tableValues, columnNames) {
            private static final long serialVersionUID = 1L;
        };
        // 创建显示表格的滚动面板
        JScrollPane scrollPane6 = new JScrollPane(browserTable);
        tabbedPane.addTab("携程酒店记录", xiechengIcon, scrollPane6, "携程酒店记录");
    }

    public void updateFlight(List<Flight> flightList) {
        String[] columnNames = {"查询时间", "城市名称", "数据状态"}; // 定义表格列名数组
        // 定义表格数据数组
        String[][] tableValues = new String[flightList.size()][3];
        for (int i = 0; i < flightList.size(); i++) {
            tableValues[i][0] = flightList.get(i).getTime();
            tableValues[i][1] = flightList.get(i).getCityName();
            tableValues[i][2] = "未删除数据";
        }
        JTable browserTable = new JTable(tableValues, columnNames) {
            private static final long serialVersionUID = 1L;
        };
        // 创建显示表格的滚动面板
        JScrollPane scrollPane7 = new JScrollPane(browserTable);
        tabbedPane.addTab("携程机票记录", xiechengIcon, scrollPane7, "携程机票记录");
    }

}
