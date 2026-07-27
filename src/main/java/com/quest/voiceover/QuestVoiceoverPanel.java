package com.quest.voiceover;

import com.quest.voiceover.modules.database.DatabaseManager;
import com.quest.voiceover.modules.database.DatabaseVersionManager;
import com.quest.voiceover.utility.PluginVersionUtility;
import net.runelite.api.Quest;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Set;

public class QuestVoiceoverPanel extends PluginPanel
{
    private static final String PLUGIN_VERSION;
    private static final String REQUEST_QUEST_URL = "https://github.com/KevinEdry/runelite-quest-voiceover/issues/new?template=quest-request.yml";
    private static final String REPORT_ISSUE_URL = "https://github.com/KevinEdry/runelite-quest-voiceover/issues/new?template=issue-report.yml";
    private static final String DISCORD_URL = "https://discord.com/invite/tkr6tEbXJr";
    private static final String KOFI_URL = "https://ko-fi.com/kedry";

    private static final int ICON_SIZE = 24;
    private static final Color KOFI_RED = new Color(0xFF, 0x5E, 0x5B);

    private static final double HEART_LOBE_RADIUS = ICON_SIZE * 0.20;
    private static final double HEART_LOBE_CENTER_Y = ICON_SIZE * 0.34;
    private static final double HEART_LEFT_LOBE_CENTER_X = ICON_SIZE * 0.32;
    private static final double HEART_RIGHT_LOBE_CENTER_X = ICON_SIZE * 0.68;
    private static final double HEART_BODY_TOP_LEFT_X = ICON_SIZE * 0.13;
    private static final double HEART_BODY_TOP_RIGHT_X = ICON_SIZE * 0.87;
    private static final double HEART_BODY_TOP_Y = ICON_SIZE * 0.40;
    private static final double HEART_BODY_TIP_X = ICON_SIZE * 0.50;
    private static final double HEART_BODY_TIP_Y = ICON_SIZE * 0.84;

    private static final ImageIcon ARROW_RIGHT_ICON;
    private static final ImageIcon GITHUB_ICON;
    private static final ImageIcon DISCORD_ICON;
    private static final ImageIcon DONATION_ICON;

    static
    {
        PLUGIN_VERSION = PluginVersionUtility.get();

        final BufferedImage arrowRight = ImageUtil.loadImageResource(QuestVoiceoverPanel.class, "arrow_right.png");
        ARROW_RIGHT_ICON = new ImageIcon(arrowRight);

        final BufferedImage githubIcon = ImageUtil.loadImageResource(QuestVoiceoverPanel.class, "github_icon.png");
        GITHUB_ICON = new ImageIcon(githubIcon);

        final BufferedImage discordIcon = ImageUtil.loadImageResource(QuestVoiceoverPanel.class, "discord_icon.png");
        DISCORD_ICON = new ImageIcon(discordIcon);

        DONATION_ICON = createHeartIcon();
    }

    private final JLabel databaseVersionLabel;
    private final JLabel connectionStatusLabel;
    private final JLabel questCoverageLabel;
    private final JLabel questPercentageLabel;

    public QuestVoiceoverPanel()
    {
        super(false);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("Quest Voiceover");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(titleLabel);
        add(Box.createVerticalStrut(10));

        // Plugin section
        JPanel pluginBox = new JPanel();
        pluginBox.setLayout(new BoxLayout(pluginBox, BoxLayout.Y_AXIS));
        pluginBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        pluginBox.setBorder(new EmptyBorder(10, 10, 10, 10));
        pluginBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel pluginHeader = new JLabel("Plugin");
        pluginHeader.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        pluginHeader.setForeground(Color.WHITE);
        pluginHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel pluginVersionLabel = new JLabel(htmlLabel("Version: ", PLUGIN_VERSION));
        pluginVersionLabel.setFont(FontManager.getRunescapeSmallFont());
        pluginVersionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        pluginVersionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        pluginBox.add(pluginHeader);
        pluginBox.add(Box.createVerticalStrut(8));
        pluginBox.add(pluginVersionLabel);

        // Database section
        JPanel databaseBox = new JPanel();
        databaseBox.setLayout(new BoxLayout(databaseBox, BoxLayout.Y_AXIS));
        databaseBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        databaseBox.setBorder(new EmptyBorder(10, 10, 10, 10));
        databaseBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel databaseHeader = new JLabel("Database");
        databaseHeader.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        databaseHeader.setForeground(Color.WHITE);
        databaseHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        databaseVersionLabel = new JLabel(htmlLabel("Version: ", "Loading..."));
        databaseVersionLabel.setFont(FontManager.getRunescapeSmallFont());
        databaseVersionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        databaseVersionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        connectionStatusLabel = new JLabel(htmlLabel("Status: ", "Loading..."));
        connectionStatusLabel.setFont(FontManager.getRunescapeSmallFont());
        connectionStatusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        connectionStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        databaseBox.add(databaseHeader);
        databaseBox.add(Box.createVerticalStrut(8));
        databaseBox.add(databaseVersionLabel);
        databaseBox.add(Box.createVerticalStrut(2));
        databaseBox.add(connectionStatusLabel);

        // Coverage section
        JPanel coverageBox = new JPanel();
        coverageBox.setLayout(new BoxLayout(coverageBox, BoxLayout.Y_AXIS));
        coverageBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        coverageBox.setBorder(new EmptyBorder(10, 10, 10, 10));
        coverageBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel coverageHeader = new JLabel("Coverage");
        coverageHeader.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        coverageHeader.setForeground(Color.WHITE);
        coverageHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        questCoverageLabel = new JLabel(htmlLabel("Quests voiced: ", "Loading..."));
        questCoverageLabel.setFont(FontManager.getRunescapeSmallFont());
        questCoverageLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        questCoverageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        questPercentageLabel = new JLabel(htmlLabel("Completion: ", "Loading..."));
        questPercentageLabel.setFont(FontManager.getRunescapeSmallFont());
        questPercentageLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        questPercentageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        coverageBox.add(coverageHeader);
        coverageBox.add(Box.createVerticalStrut(8));
        coverageBox.add(questCoverageLabel);
        coverageBox.add(Box.createVerticalStrut(2));
        coverageBox.add(questPercentageLabel);

        add(pluginBox);
        add(Box.createVerticalStrut(10));
        add(databaseBox);
        add(Box.createVerticalStrut(10));
        add(coverageBox);
        add(Box.createVerticalStrut(10));
        add(buildLinkPanel(GITHUB_ICON, "Request a", "new quest", REQUEST_QUEST_URL));
        add(Box.createVerticalStrut(10));
        add(buildLinkPanel(GITHUB_ICON, "Report an issue or", "make a suggestion", REPORT_ISSUE_URL));
        add(Box.createVerticalStrut(10));
        add(buildLinkPanel(DISCORD_ICON, "Talk to us on our", "Discord server", DISCORD_URL));
        add(Box.createVerticalStrut(10));
        add(buildLinkPanel(DONATION_ICON, "Support the plugin", "on Ko-fi", KOFI_URL));
        add(Box.createVerticalGlue());
    }

    public void updateInfo(DatabaseManager databaseManager, Set<String> voicedQuests)
    {
        String dbVersion = DatabaseVersionManager.getDatabaseVersion();
        databaseVersionLabel.setText(htmlLabel("Version: ", dbVersion));

        boolean connected = databaseManager.isConnected();
        String statusText = connected ? "Connected" : "Disconnected";
        connectionStatusLabel.setText(htmlLabel("Status: ", statusText));

        int totalQuests = Quest.values().length;
        int voicedCount = voicedQuests != null ? voicedQuests.size() : 0;
        String coverageText = voicedCount + " / " + totalQuests;
        questCoverageLabel.setText(htmlLabel("Quests voiced: ", coverageText));

        double percentage = (voicedCount * 100.0) / totalQuests;
        String percentageText = String.format("%.1f%%", percentage);
        questPercentageLabel.setText(htmlLabel("Completion: ", percentageText));
    }

    private static String htmlLabel(String key, String value)
    {
        return "<html><body>" + key + "<span style='color:white'>" + value + "</span></body></html>";
    }

    /**
     * Drawn at runtime rather than shipped as an asset so the donation button needs no
     * bundled image and carries no third-party logo/trademark.
     */
    private static ImageIcon createHeartIcon()
    {
        BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(KOFI_RED);

        double lobeDiameter = HEART_LOBE_RADIUS * 2;
        graphics.fill(new Ellipse2D.Double(
            HEART_LEFT_LOBE_CENTER_X - HEART_LOBE_RADIUS, HEART_LOBE_CENTER_Y - HEART_LOBE_RADIUS,
            lobeDiameter, lobeDiameter));
        graphics.fill(new Ellipse2D.Double(
            HEART_RIGHT_LOBE_CENTER_X - HEART_LOBE_RADIUS, HEART_LOBE_CENTER_Y - HEART_LOBE_RADIUS,
            lobeDiameter, lobeDiameter));

        Path2D.Double body = new Path2D.Double();
        body.moveTo(HEART_BODY_TOP_LEFT_X, HEART_BODY_TOP_Y);
        body.lineTo(HEART_BODY_TOP_RIGHT_X, HEART_BODY_TOP_Y);
        body.lineTo(HEART_BODY_TIP_X, HEART_BODY_TIP_Y);
        body.closePath();
        graphics.fill(body);

        graphics.dispose();
        return new ImageIcon(image);
    }

    private static JPanel buildLinkPanel(ImageIcon icon, String topText, String bottomText, String url)
    {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        container.setBorder(new EmptyBorder(8, 10, 8, 10));
        container.setCursor(new Cursor(Cursor.HAND_CURSOR));
        container.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel iconLabel = new JLabel(icon);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        textPanel.setBorder(new EmptyBorder(0, 10, 0, 0));

        JLabel topLabel = new JLabel(topText);
        topLabel.setForeground(Color.WHITE);
        topLabel.setFont(FontManager.getRunescapeSmallFont());

        JLabel bottomLabel = new JLabel(bottomText);
        bottomLabel.setForeground(Color.WHITE);
        bottomLabel.setFont(FontManager.getRunescapeSmallFont());

        textPanel.add(topLabel);
        textPanel.add(bottomLabel);

        JLabel arrowLabel = new JLabel(ARROW_RIGHT_ICON);

        container.add(iconLabel, BorderLayout.WEST);
        container.add(textPanel, BorderLayout.CENTER);
        container.add(arrowLabel, BorderLayout.EAST);

        container.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e)
            {
                LinkBrowser.browse(url);
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e)
            {
                container.setBackground(ColorScheme.DARK_GRAY_COLOR);
                textPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e)
            {
                container.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                textPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }
        });

        return container;
    }
}
