package com.reactnativenavigation.layouts;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RelativeLayout;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.reactnativenavigation.NavigationApplication;
import com.reactnativenavigation.events.EventBus;
import com.reactnativenavigation.events.ScreenChangedEvent;
import com.reactnativenavigation.params.ContextualMenuParams;
import com.reactnativenavigation.params.FabParams;
import com.reactnativenavigation.params.LightBoxParams;
import com.reactnativenavigation.params.ScreenParams;
import com.reactnativenavigation.params.SideMenuParams;
import com.reactnativenavigation.params.SlidingOverlayParams;
import com.reactnativenavigation.params.SnackbarParams;
import com.reactnativenavigation.params.TitleBarButtonParams;
import com.reactnativenavigation.params.TitleBarLeftButtonParams;
import com.reactnativenavigation.screens.NavigationType;
import com.reactnativenavigation.screens.Screen;
import com.reactnativenavigation.screens.ScreenStack;
import com.reactnativenavigation.views.LeftButtonOnClickListener;
import com.reactnativenavigation.views.LightBox;
import com.reactnativenavigation.views.SideMenu;
import com.reactnativenavigation.views.SideMenu.Side;
import com.reactnativenavigation.views.SnackbarAndFabContainer;
import com.reactnativenavigation.views.slidingOverlay.SlidingOverlay;
import com.reactnativenavigation.views.slidingOverlay.SlidingOverlaysQueue;

import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class SingleScreenLayout extends BaseLayout {

    protected final ScreenParams screenParams;
    private final SideMenuParams leftSideMenuParams;
    private final SideMenuParams rightSideMenuParams;
    protected ScreenStack stack;
    private SnackbarAndFabContainer snackbarAndFabContainer;
    protected LeftButtonOnClickListener leftButtonOnClickListener;
    private @Nullable SideMenu sideMenu;
    private final SlidingOverlaysQueue slidingOverlaysQueue = new SlidingOverlaysQueue();
    private LightBox lightBox;

    public SingleScreenLayout(AppCompatActivity activity, SideMenuParams leftSideMenuParams,
                              SideMenuParams rightSideMenuParams, ScreenParams screenParams) {
        super(activity);
        this.screenParams = screenParams;
        this.leftSideMenuParams = leftSideMenuParams;
        this.rightSideMenuParams = rightSideMenuParams;
        createLayout();
    }

    private void createLayout() {
        if (leftSideMenuParams == null && rightSideMenuParams == null) {
            createStack(getScreenStackParent());
        } else {
            sideMenu = createSideMenu();
            createStack(getScreenStackParent());
        }
        createFabAndSnackbarContainer();
        sendScreenChangedEventAfterInitialPush();
    }

    private RelativeLayout getScreenStackParent() {
        return sideMenu == null ? this : sideMenu.getContentContainer();
    }

    private SideMenu createSideMenu() {
        SideMenu sideMenu = new SideMenu(getContext(), leftSideMenuParams, rightSideMenuParams);
        RelativeLayout.LayoutParams lp = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
        addView(sideMenu, lp);
        return sideMenu;
    }

    private void createStack(RelativeLayout parent) {
        if (stack != null) {
            stack.destroy();
        }
        stack = new ScreenStack(getActivity(), parent, screenParams.getNavigatorId(), this);
        LayoutParams lp = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
        pushInitialScreen(lp);
        pushAdditionalScreens(lp);
    }

    protected void pushInitialScreen(LayoutParams lp) {
        stack.pushInitialScreen(screenParams, lp);
    }

    protected void pushAdditionalScreens(LayoutParams lp) {
        for (ScreenParams screen : screenParams.screens) {
            stack.pushInitialScreen(screen, lp);
        }
        stack.show(NavigationType.Push);
    }

    private void sendScreenChangedEventAfterInitialPush() {
        if (screenParams.topTabParams != null) {
            EventBus.instance.post(new ScreenChangedEvent(screenParams.topTabParams.get(0)));
        } else {
            EventBus.instance.post(new ScreenChangedEvent(screenParams));
        }
    }

    private void createFabAndSnackbarContainer() {
        snackbarAndFabContainer = new SnackbarAndFabContainer(getContext(), this);
        RelativeLayout.LayoutParams lp = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
        lp.addRule(ALIGN_PARENT_BOTTOM);
        snackbarAndFabContainer.setLayoutParams(lp);
        getScreenStackParent().addView(snackbarAndFabContainer);
    }

    @Override
    public boolean onBackPressed() {
        if (handleBackInJs()) {
            return true;
        }

        if (stack.canPop()) {
            stack.pop(true, System.currentTimeMillis());
            EventBus.instance.post(new ScreenChangedEvent(stack.peek().getScreenParams()));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean handleBackInJs() {
        return stack.handleBackPressInJs();
    }

    @Override
    public void destroy() {
        stack.destroy();
        snackbarAndFabContainer.destroy();
        if (sideMenu != null) {
            sideMenu.destroy();
        }
        if (sideMenu != null) {
            sideMenu.destroy();
        }
        if (lightBox != null) {
            lightBox.destroy();
        }
        slidingOverlaysQueue.destroy();
    }

    @Override
    public void push(ScreenParams params, Promise onPushComplete) {
        stack.push(params, new LayoutParams(MATCH_PARENT, MATCH_PARENT), onPushComplete);
        EventBus.instance.post(new ScreenChangedEvent(params));
    }

    @Override
    public void pop(ScreenParams params) {
        stack.pop(params.animateScreenTransitions, params.timestamp, new ScreenStack.OnScreenPop() {
            @Override
            public void onScreenPopAnimationEnd() {
                EventBus.instance.post(new ScreenChangedEvent(stack.peek().getScreenParams()));
            }
        });
    }

    @Override
    public void popToRoot(ScreenParams params) {
        stack.popToRoot(params.animateScreenTransitions, params.timestamp, new ScreenStack.OnScreenPop() {
            @Override
            public void onScreenPopAnimationEnd() {
                EventBus.instance.post(new ScreenChangedEvent(stack.peek().getScreenParams()));
            }
        });
    }

    @Override
    public void newStack(final ScreenParams params) {
        stack.newStack(params, new LayoutParams(MATCH_PARENT, MATCH_PARENT));
        EventBus.instance.post(new ScreenChangedEvent(params));
    }

    @Override
    public void setTopBarVisible(String screenInstanceID, boolean visible, boolean animate) {
        stack.setScreenTopBarVisible(screenInstanceID, visible, animate);
    }

    @Override
    public void setTitleBarTitle(String screenInstanceId, String title) {
        stack.setScreenTitleBarTitle(screenInstanceId, title);
    }

    @Override
    public void setTitleBarSubtitle(String screenInstanceId, String subtitle) {
        stack.setScreenTitleBarSubtitle(screenInstanceId, subtitle);
    }

    @Override
    public View asView() {
        return this;
    }

    @Override
    public void setTitleBarRightButtons(String screenInstanceId, String navigatorEventId,
                                        List<TitleBarButtonParams> titleBarRightButtons) {
        stack.setScreenTitleBarRightButtons(screenInstanceId, navigatorEventId, titleBarRightButtons);
    }

    @Override
    public void setTitleBarLeftButton(String screenInstanceId, String navigatorEventId, TitleBarLeftButtonParams titleBarLeftButtonParams) {
        stack.setScreenTitleBarLeftButton(screenInstanceId, navigatorEventId, titleBarLeftButtonParams);
    }

    @Override
    public void setFab(String screenInstanceId, String navigatorEventId, FabParams fabParams) {
        stack.setFab(screenInstanceId, fabParams);
    }

    @Override
    public void toggleSideMenuVisible(boolean animated, Side side) {
        if (sideMenu != null) {
            sideMenu.toggleVisible(animated, side);
        }
    }

    @Override
    public void setSideMenuVisible(boolean animated, boolean visible, Side side) {
        if (sideMenu != null) {
            sideMenu.setVisible(visible, animated, side);
        }
    }

    @Override
    public void setSideMenuEnabled(boolean enabled, Side side) {
        if (sideMenu != null) {
            sideMenu.setEnabled(enabled, side);
        }
    }

    @Override
    public void showSnackbar(SnackbarParams params) {
        final String navigatorEventId = stack.peek().getNavigatorEventId();
        snackbarAndFabContainer.showSnackbar(navigatorEventId, params);
    }

    @Override
    public void dismissSnackbar() {
        snackbarAndFabContainer.dismissSnackbar();
    }

    @Override
    public void showLightBox(LightBoxParams params) {
        // if (lightBox == null) {
        if (lightBox == null || !lightBox.isShowing()) {
            lightBox = new LightBox(getActivity(), new Runnable() {
                @Override
                public void run() {
                    // lightBox = null;
                }
            }, params);
            lightBox.show();
        }
    }

    @Override
    public void dismissLightBox() {
        if (lightBox != null) {
            lightBox.hide();
            lightBox = null;
        }
    }

    @Override
    public void selectTopTabByTabIndex(String screenInstanceId, int index) {
        stack.selectTopTabByTabIndex(screenInstanceId, index);
    }

    @Override
    public void selectTopTabByScreen(String screenInstanceId) {
        stack.selectTopTabByScreen(screenInstanceId);
    }

    @Override
    public void updateScreenStyle(String screenInstanceId, Bundle styleParams) {
        stack.updateScreenStyle(screenInstanceId, styleParams);
    }

    @Override
    public String getCurrentlyVisibleScreenId() {
        return stack.peek().getScreenInstanceId();
    }

    @Override
    public void showSlidingOverlay(final SlidingOverlayParams params) {
        slidingOverlaysQueue.add(new SlidingOverlay(this, params));
    }

    @Override
    public void hideSlidingOverlay() {
        slidingOverlaysQueue.remove();
    }

    @Override
    public void onModalDismissed() {
        stack.peek().setStyle();
        stack.peek().getScreenParams().timestamp = System.currentTimeMillis();
        NavigationApplication.instance.getEventEmitter().sendWillAppearEvent(stack.peek().getScreenParams(), NavigationType.DismissModal);
        NavigationApplication.instance.getEventEmitter().sendDidAppearEvent(stack.peek().getScreenParams(), NavigationType.DismissModal);
        EventBus.instance.post(new ScreenChangedEvent(stack.peek().getScreenParams()));
    }

    @Override
    public boolean containsNavigator(String navigatorId) {
        return stack.getNavigatorId().equals(navigatorId);
    }

    @Override
    public void showContextualMenu(String screenInstanceId, ContextualMenuParams params, Callback onButtonClicked) {
        stack.showContextualMenu(screenInstanceId, params, onButtonClicked);
    }

    @Override
    public void dismissContextualMenu(String screenInstanceId) {
        stack.dismissContextualMenu(screenInstanceId);
    }

    @Override
    public Screen getCurrentScreen() {
        return stack.peek();
    }

    @Override
    public boolean onTitleBarBackButtonClick() {
        if (leftButtonOnClickListener != null) {
            return leftButtonOnClickListener.onTitleBarBackButtonClick();
        }

        return onBackPressed();
    }

    @Override
    public void onSideMenuButtonClick() {
        final String navigatorEventId = stack.peek().getNavigatorEventId();
        NavigationApplication.instance.getEventEmitter().sendNavigatorEvent("sideMenu", navigatorEventId);
        if (sideMenu != null) {
            sideMenu.openDrawer(Side.Left);
        }
    }
}
