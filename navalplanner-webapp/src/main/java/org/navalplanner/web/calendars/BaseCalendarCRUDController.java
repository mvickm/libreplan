package org.navalplanner.web.calendars;

import static org.navalplanner.web.I18nHelper._;

import java.util.Date;

import org.navalplanner.business.calendars.entities.BaseCalendar;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.web.common.IMessagesForUser;
import org.navalplanner.web.common.Level;
import org.navalplanner.web.common.MessagesForUser;
import org.navalplanner.web.common.OnlyOneVisible;
import org.navalplanner.web.common.Util;
import org.navalplanner.web.common.components.CalendarHighlightedDays;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.SimpleTreeNode;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.api.Window;

/**
 * Controller for CRUD actions over a {@link BaseCalendar}
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class BaseCalendarCRUDController extends GenericForwardComposer {

    private IBaseCalendarModel baseCalendarModel;

    private Window listWindow;

    private Window createWindow;

    private Window editWindow;

    private Window confirmRemove;

    private Window createNewVersion;

    private boolean confirmingRemove = false;

    private OnlyOneVisible visibility;

    private IMessagesForUser messagesForUser;

    private Component messagesContainer;

    private BaseCalendarsTreeitemRenderer baseCalendarsTreeitemRenderer = new BaseCalendarsTreeitemRenderer();

    private BaseCalendarEditionController createController;

    private BaseCalendarEditionController editionController;

    public BaseCalendar getBaseCalendar() {
        return baseCalendarModel.getBaseCalendar();
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        messagesForUser = new MessagesForUser(messagesContainer);
        comp.setVariable("controller", this, true);
        getVisibility().showOnly(listWindow);
    }

    public void cancel() {
        baseCalendarModel.cancel();
        goToList();
    }

    public void goToList() {
        Util.reloadBindings(listWindow);
        getVisibility().showOnly(listWindow);
    }

    public void goToEditForm(BaseCalendar baseCalendar) {
        baseCalendarModel.initEdit(baseCalendar);
        assignEditionController();
        setSelectedDay(new Date());
        highlightDaysOnCalendar();
        getVisibility().showOnly(editWindow);
        Util.reloadBindings(editWindow);
    }

    private void highlightDaysOnCalendar() {
        if (baseCalendarModel.isEditing()) {
            ((CalendarHighlightedDays) editWindow.getFellow("calendarWidget"))
                    .highlightDays();
        } else {
            ((CalendarHighlightedDays) createWindow.getFellow("calendarWidget"))
                    .highlightDays();
        }
    }

    public void save() {
        try {
            baseCalendarModel.confirmSave();
            messagesForUser.showMessage(Level.INFO, _(
                    "Base calendar \"{0}\" saved", baseCalendarModel
                            .getBaseCalendar().getName()));
            goToList();
        } catch (ValidationException e) {
            messagesForUser.showInvalidValues(e);
        }
    }

    public void confirmRemove(BaseCalendar baseCalendar) {
        baseCalendarModel.initRemove(baseCalendar);
        showConfirmingWindow();
    }

    public void cancelRemove() {
        confirmingRemove = false;
        baseCalendarModel.cancel();
        confirmRemove.setVisible(false);
        Util.reloadBindings(confirmRemove);
    }

    public boolean isConfirmingRemove() {
        return confirmingRemove;
    }

    private void hideConfirmingWindow() {
        confirmingRemove = false;
        Util.reloadBindings(confirmRemove);
    }

    private void showConfirmingWindow() {
        confirmingRemove = true;
        try {
            Util.reloadBindings(confirmRemove);
            confirmRemove.doModal();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void remove() {
        String name = baseCalendarModel.getBaseCalendar().getName();
        if (baseCalendarModel.isParent()) {
            hideConfirmingWindow();
            messagesForUser
                    .showMessage(Level.ERROR,
                            _("The calendar was not removed because it still has children. "
                                    + "Some other calendar is derived from this."));
        } else {
            baseCalendarModel.confirmRemove();
            hideConfirmingWindow();
            Util.reloadBindings(listWindow);
            messagesForUser.showMessage(Level.INFO, _(
                    "Removed calendar \"{0}\"", name));
        }
    }

    public void goToCreateForm() {
        baseCalendarModel.initCreate();
        assignCreateController();
        setSelectedDay(new Date());
        highlightDaysOnCalendar();
        getVisibility().showOnly(createWindow);
        Util.reloadBindings(createWindow);
    }

    public void setSelectedDay(Date date) {
        baseCalendarModel.setSelectedDay(date);

        reloadDayInformation();
    }

    private void assignEditionController() {
        editionController = new BaseCalendarEditionController(
                baseCalendarModel, editWindow, createNewVersion) {

            @Override
            protected void goToList() {
                BaseCalendarCRUDController.this.goToList();
            }

        };

        try {
            editionController.doAfterCompose(editWindow);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assignCreateController() {
        createController = new BaseCalendarEditionController(baseCalendarModel,
                createWindow, createNewVersion) {

            @Override
            protected void goToList() {
                BaseCalendarCRUDController.this.goToList();
            }

        };

        try {
            createController.doAfterCompose(createWindow);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private OnlyOneVisible getVisibility() {
        if (visibility == null) {
            visibility = new OnlyOneVisible(listWindow, createWindow,
                    editWindow);
        }
        return visibility;
    }

    private void reloadCurrentWindow() {
        if (baseCalendarModel.isEditing()) {
            Util.reloadBindings(editWindow);
        } else {
            Util.reloadBindings(createWindow);
        }
        highlightDaysOnCalendar();
    }

    private void reloadDayInformation() {
        if (baseCalendarModel.isEditing()) {
            Util.reloadBindings(editWindow.getFellow("dayInformation"));
        } else {
            Util.reloadBindings(createWindow.getFellow("dayInformation"));
        }
        highlightDaysOnCalendar();
    }

    public void goToCreateDerivedForm(BaseCalendar baseCalendar) {
        baseCalendarModel.initCreateDerived(baseCalendar);
        assignCreateController();
        setSelectedDay(new Date());
        highlightDaysOnCalendar();
        getVisibility().showOnly(createWindow);
        Util.reloadBindings(createWindow);
    }

    public boolean isEditing() {
        return baseCalendarModel.isEditing();
    }

    public void goToCreateCopyForm(BaseCalendar baseCalendar) {
        baseCalendarModel.initCreateCopy(baseCalendar);
        assignCreateController();
        setSelectedDay(new Date());
        highlightDaysOnCalendar();
        getVisibility().showOnly(createWindow);
        Util.reloadBindings(createWindow);
    }

    public BaseCalendarsTreeModel getBaseCalendarsTreeModel() {
        return new BaseCalendarsTreeModel(new BaseCalendarTreeRoot(
                baseCalendarModel.getBaseCalendars()));
    }

    public BaseCalendarsTreeitemRenderer getBaseCalendarsTreeitemRenderer() {
        return baseCalendarsTreeitemRenderer;
    }

    public class BaseCalendarsTreeitemRenderer implements TreeitemRenderer {

        @Override
        public void render(Treeitem item, Object data) throws Exception {
            SimpleTreeNode simpleTreeNode = (SimpleTreeNode) data;
            final BaseCalendar baseCalendar = (BaseCalendar) simpleTreeNode
                    .getData();
            item.setValue(data);

            Treerow treerow = new Treerow();

            Treecell nameTreecell = new Treecell();
            Label nameLabel = new Label(baseCalendar.getName());
            nameTreecell.appendChild(nameLabel);
            treerow.appendChild(nameTreecell);

            Treecell operationsTreecell = new Treecell();

            Button createDerivedButton = new Button(_("Create derived"));
            createDerivedButton.addEventListener(Events.ON_CLICK,
                    new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    goToCreateDerivedForm(baseCalendar);
                }

            });
            operationsTreecell.appendChild(createDerivedButton);

            Button createCopyButton = new Button(_("Create copy"));
            createCopyButton.addEventListener(Events.ON_CLICK,
                    new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    goToCreateCopyForm(baseCalendar);
                }

            });
            operationsTreecell.appendChild(createCopyButton);

            Button editButton = new Button(_("Edit"));
            editButton.addEventListener(Events.ON_CLICK, new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    goToEditForm(baseCalendar);
                }

            });
            operationsTreecell.appendChild(editButton);

            Button removeButton = new Button(_("Remove"));
            removeButton.addEventListener(Events.ON_CLICK, new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    confirmRemove(baseCalendar);
                }

            });
            operationsTreecell.appendChild(removeButton);

            treerow.appendChild(operationsTreecell);

            item.appendChild(treerow);

            // Show the tree expanded at start
            item.setOpen(true);
        }

    }

    public BaseCalendarEditionController getEditionController() {
        if (isEditing()) {
            return editionController;
        } else {
            return createController;
        }
    }

}
