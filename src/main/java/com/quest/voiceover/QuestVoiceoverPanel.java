package com.quest.voiceover;

import com.quest.voiceover.modules.database.DatabaseManager;
import com.quest.voiceover.modules.database.DatabaseVersionManager;
import net.runelite.api.Quest;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

public class QuestVoiceoverPanel extends PluginPanel
{
    private static final String PLUGIN_VERSION;
    private static final String REQUEST_QUEST_URL = "https://github.com/KevinEdry/runelite-quest-voiceover/issues/new?template=quest-request.yml";
    private static final String REPORT_ISSUE_URL = "https://github.com/KevinEdry/runelite-quest-voiceover/issues/new?template=issue-report.yml";
    private static final String DISCORD_URL = "https://discord.com/invite/tkr6tEbXJr";

    private static final ImageIcon ARROW_RIGHT_ICON;
    private static final ImageIcon GITHUB_ICON;
    private static final ImageIcon DISCORD_ICON;

    static
    {
        String version = "Unknown";
        try (InputStream input = QuestVoiceoverPanel.class.getResourceAsStream("version.properties"))
        {
            if (input != null)
            {
                Properties props = new Properties();
                props.load(input);
                version = props.getProperty("version", "Unknown");
            }
        }
        catch (IOException ignored)
        {
        }
        PLUGIN_VERSION = version;

        final BufferedImage arrowRight = ImageUtil.loadImageResource(QuestVoiceoverPanel.class, "arrow_right.png");
        ARROW_RIGHT_ICON = new ImageIcon(arrowRight);

        final BufferedImage githubIcon = ImageUtil.loadImageResource(QuestVoiceoverPanel.class, "github_icon.png");
        GITHUB_ICON = new ImageIcon(githubIcon);

        final BufferedImage discordIcon = ImageUtil.loadImageResource(QuestVoiceoverPanel.class, "discord_icon.png");
        DISCORD_ICON = new ImageIcon(discordIcon);
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
