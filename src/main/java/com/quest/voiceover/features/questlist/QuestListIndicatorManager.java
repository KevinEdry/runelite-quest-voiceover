package com.quest.voiceover.features.questlist;

import net.runelite.api.Client;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class QuestListIndicatorManager {

    private static final int QUEST_LIST_CONTAINER_COMPONENT = 7;
    private static final int WIDGET_TYPE_TEXT = 4;
    private static final int TICKS_BEFORE_ADDING_INDICATORS = 2;
    private static final String VOICE_INDICATOR_PREFIX = "[Voiced] ";

    @Inject
    private Client client;

    private Set<String> voicedQuests = new HashSet<>();
    private Set<String> questsAlreadyMarked = new HashSet<>();
    private boolean isQuestListVisible;
    private int ticksSinceOpened;

    public void setVoicedQuests(Set<String> voicedQuests) {
        this.voicedQuests = voicedQuests;
    }

    public void onQuestListOpened() {
        isQuestListVisible = true;
        ticksSinceOpened = 0;
        questsAlreadyMarked.clear();
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
            addVoiceIndicators();
        }
    }

    private void addVoiceIndicators() {
        if (voicedQuests.isEmpty() || !isQuestListActuallyVisible()) {
            return;
        }

        Widget questListContainer = client.getWidget(InterfaceID.QUEST_LIST, QUEST_LIST_CONTAINER_COMPONENT);
        if (questListContainer == null || questListContainer.getChildren() == null) {
            return;
        }

        markVoicedQuests(questListContainer.getChildren());
    }

    private boolean isQuestListActuallyVisible() {
        Widget questListPanel = client.getWidget(InterfaceID.QUEST_LIST, 0);

        if (questListPanel == null || questListPanel.isHidden()) {
            return false;
        }

        return questListPanel.getCanvasLocation() != null && questListPanel.getWidth() > 0;
    }

    private void markVoicedQuests(Widget[] questWidgets) {
        for (Widget questWidget : questWidgets) {
            if (!isTextWidget(questWidget)) {
                continue;
            }

            String questName = questWidget.getText();

            if (shouldMarkAsVoiced(questName)) {
                questWidget.setText(VOICE_INDICATOR_PREFIX + questName);
                questWidget.revalidate();
                questsAlreadyMarked.add(questName);
            }
        }
    }

    private boolean isTextWidget(Widget widget) {
        return widget != null && widget.getType() == WIDGET_TYPE_TEXT;
    }

    private boolean shouldMarkAsVoiced(String questName) {
        return questName != null
            && voicedQuests.contains(questName)
            && !questsAlreadyMarked.contains(questName);
    }
}
