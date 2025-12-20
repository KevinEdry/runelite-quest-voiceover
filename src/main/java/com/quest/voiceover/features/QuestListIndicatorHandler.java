package com.quest.voiceover.features;

import com.quest.voiceover.QuestVoiceoverConfig;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class QuestListIndicatorHandler {

    private static final int QUEST_LIST_CONTAINER_COMPONENT = 7;
    private static final int WIDGET_TYPE_TEXT = 4;
    private static final int TICKS_BEFORE_ADDING_INDICATORS = 2;
    private static final String SHORT_PREFIX = "[V] ";
    private static final String LONG_PREFIX = "[Voiced] ";

    @Inject
    private Client client;

    @Inject
    private QuestVoiceoverConfig config;

    private Set<String> voicedQuests = new HashSet<>();
    private boolean isQuestListVisible;
    private int ticksSinceOpened;

    public void setVoicedQuests(Set<String> voicedQuests) {
        this.voicedQuests = voicedQuests;
    }

    public void onQuestListOpened() {
        isQuestListVisible = true;
        ticksSinceOpened = 0;
    }

    public void onQuestListClosed() {
        isQuestListVisible = false;
    }

    public void onGameTick() {
        if (!isQuestListVisible) {
            return;
        }

        ticksSinceOpened++;

        if (ticksSinceOpened >= TICKS_BEFORE_ADDING_INDICATORS) {
            if (config.showVoicedIndicator()) {
                updateVoiceIndicators();
            } else {
                removeVoiceIndicators();
            }
        }
    }

    private void updateVoiceIndicators() {
        if (voicedQuests.isEmpty() || !isQuestListActuallyVisible()) {
            return;
        }

        Widget questListContainer = client.getWidget(InterfaceID.QUEST_LIST, QUEST_LIST_CONTAINER_COMPONENT);
        if (questListContainer == null || questListContainer.getChildren() == null) {
            return;
        }

        Point mousePosition = client.getMouseCanvasPosition();
        updateQuestWidgets(questListContainer.getChildren(), mousePosition);
    }

    private void removeVoiceIndicators() {
        if (!isQuestListActuallyVisible()) {
            return;
        }

        Widget questListContainer = client.getWidget(InterfaceID.QUEST_LIST, QUEST_LIST_CONTAINER_COMPONENT);
        if (questListContainer == null || questListContainer.getChildren() == null) {
            return;
        }

        for (Widget questWidget : questListContainer.getChildren()) {
            if (!isTextWidget(questWidget)) {
                continue;
            }

            String currentText = questWidget.getText();
            if (hasVoicePrefix(currentText)) {
                questWidget.setText(extractQuestName(currentText));
                questWidget.revalidate();
            }
        }
    }

    private boolean hasVoicePrefix(String text) {
        return text != null && (text.startsWith(SHORT_PREFIX) || text.startsWith(LONG_PREFIX));
    }

    private boolean isQuestListActuallyVisible() {
        Widget questListPanel = client.getWidget(InterfaceID.QUEST_LIST, 0);

        if (questListPanel == null || questListPanel.isHidden()) {
            return false;
        }

        return questListPanel.getCanvasLocation() != null && questListPanel.getWidth() > 0;
    }

    private void updateQuestWidgets(Widget[] questWidgets, Point mousePosition) {
        for (Widget questWidget : questWidgets) {
            if (!isTextWidget(questWidget)) {
                continue;
            }

            String currentText = questWidget.getText();
            String questName = extractQuestName(currentText);

            if (!isVoicedQuest(questName)) {
                continue;
            }

            boolean isHovered = isMouseOverWidget(questWidget, mousePosition);
            String expectedPrefix = isHovered ? LONG_PREFIX : SHORT_PREFIX;
            String expectedText = expectedPrefix + questName;

            if (!currentText.equals(expectedText)) {
                questWidget.setText(expectedText);
                questWidget.revalidate();
            }
        }
    }

    private String extractQuestName(String text) {
        if (text == null) {
            return null;
        }
        if (text.startsWith(LONG_PREFIX)) {
            return text.substring(LONG_PREFIX.length());
        }
        if (text.startsWith(SHORT_PREFIX)) {
            return text.substring(SHORT_PREFIX.length());
        }
        return text;
    }

    private boolean isVoicedQuest(String questName) {
        return questName != null && voicedQuests.contains(questName);
    }

    private boolean isMouseOverWidget(Widget widget, Point mousePosition) {
        Rectangle bounds = widget.getBounds();
        if (bounds == null || mousePosition == null) {
            return false;
        }
        return bounds.contains(mousePosition.getX(), mousePosition.getY());
    }

    private boolean isTextWidget(Widget widget) {
        return widget != null && widget.getType() == WIDGET_TYPE_TEXT;
    }
}
