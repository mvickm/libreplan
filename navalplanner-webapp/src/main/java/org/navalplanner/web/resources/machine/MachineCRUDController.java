/*
 * This file is part of ###PROJECT_NAME###
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.navalplanner.web.resources.machine;

import static org.navalplanner.web.I18nHelper._;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.InvalidValue;
import org.navalplanner.business.calendars.entities.BaseCalendar;
import org.navalplanner.business.calendars.entities.ResourceCalendar;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.resources.entities.Machine;
import org.navalplanner.web.calendars.BaseCalendarEditionController;
import org.navalplanner.web.calendars.IBaseCalendarModel;
import org.navalplanner.web.common.IMessagesForUser;
import org.navalplanner.web.common.Level;
import org.navalplanner.web.common.MessagesForUser;
import org.navalplanner.web.common.OnlyOneVisible;
import org.navalplanner.web.common.Util;
import org.navalplanner.web.common.entrypoints.IURLHandlerRegistry;
import org.navalplanner.web.costcategories.ResourcesCostCategoryAssignmentController;
import org.navalplanner.web.resources.worker.CriterionsController;
import org.navalplanner.web.resources.worker.CriterionsMachineController;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Tab;
import org.zkoss.zul.api.Window;

/**
 * Controller for {@link Machine} resource <br />
 * @author Diego Pino Garcia <dpino@igalia.com>
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 */
public class MachineCRUDController extends GenericForwardComposer {

    private Window listWindow;

    private Window editWindow;

    private IMachineModel machineModel;

    private IURLHandlerRegistry URLHandlerRegistry;

    private OnlyOneVisible visibility;

    private IMessagesForUser messagesForUser;

    private Component messagesContainer;

    private Component configurationUnits;

    private CriterionsMachineController criterionsController;

    private MachineConfigurationController configurationController;

    private ResourcesCostCategoryAssignmentController resourcesCostCategoryAssignmentController;

    private static final Log LOG = LogFactory
            .getLog(MachineCRUDController.class);

    public MachineCRUDController() {

    }

    private BaseCalendarsComboitemRenderer baseCalendarsComboitemRenderer = new BaseCalendarsComboitemRenderer();

    public List<Machine> getMachines() {
        return machineModel.getMachines();
    }

    public Machine getMachine() {
        return machineModel.getMachine();
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        comp.setVariable("controller", this, true);
        messagesForUser = new MessagesForUser(messagesContainer);
        setupCriterionsController();
        setupConfigurationController();
        setupResourcesCostCategoryAssignmentController(comp);
        showListWindow();
    }

    private void showListWindow() {
        getVisibility().showOnly(listWindow);
    }

    private OnlyOneVisible getVisibility() {
        if (visibility == null) {
            visibility = new OnlyOneVisible(listWindow, editWindow);
        }
        return visibility;
    }

    private void setupCriterionsController() throws Exception {
        final Component comp = editWindow.getFellowIfAny("criterionsContainer");
        criterionsController = new CriterionsMachineController();
        criterionsController.doAfterCompose(comp);
    }

    private void setupConfigurationController() throws Exception {
        configurationUnits = editWindow.getFellow("configurationUnits");
        configurationController = (MachineConfigurationController) configurationUnits
                .getVariable("configurationController", true);
    }

    private void setupResourcesCostCategoryAssignmentController(Component comp)
    throws Exception {
        Component costCategoryAssignmentContainer =
            editWindow.getFellowIfAny("costCategoryAssignmentContainer");
        resourcesCostCategoryAssignmentController = (ResourcesCostCategoryAssignmentController)
            costCategoryAssignmentContainer.getVariable("assignmentController", true);
    }

    public void goToCreateForm() {
        machineModel.initCreate();
        criterionsController.prepareForCreate(machineModel.getMachine());
        configurationController.initConfigurationController(machineModel);
        resourcesCostCategoryAssignmentController.setResource(machineModel.getMachine());
        selectMachineDataTab();
        showEditWindow(_("Create machine"));
    }

    private void showEditWindow(String title) {
        editWindow.setTitle(title);
        showEditWindow();
    }

    private void showEditWindow() {
        getVisibility().showOnly(editWindow);
        Util.reloadBindings(editWindow);
    }

    /**
     * Loads {@link Machine} into model, shares loaded {@link Machine} with
     * {@link CriterionsController}
     *
     * @param machine
     */
    public void goToEditForm(Machine machine) {
        machineModel.initEdit(machine);
        prepareCriterionsForEdit();
        prepareCalendarForEdit();
        selectMachineDataTab();
        showEditWindow(_("Edit machine"));
        configurationController.initConfigurationController(machineModel);
        resourcesCostCategoryAssignmentController.setResource(machineModel.getMachine());
    }

    private void selectMachineDataTab() {
        Tab tabMachineData = (Tab) editWindow.getFellow("tbMachineData");
        tabMachineData.setSelected(true);
    }

    private void prepareCriterionsForEdit() {
        criterionsController.prepareForEdit(machineModel.getMachine());
    }

    private void prepareCalendarForEdit() {
        if (isCalendarNull()) {
            return;
        }

        updateCalendarController();
        resourceCalendarModel.initEdit(machineModel.getCalendarOfMachine());
        try {
            baseCalendarEditionController.doAfterCompose(editCalendarWindow);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        baseCalendarEditionController.setSelectedDay(new Date());
        Util.reloadBindings(editCalendarWindow);
        Util.reloadBindings(createNewVersionWindow);
    }

    public void save() {
        try {
            saveCalendar();
            saveCriterions();
            machineModel.confirmSave();
            goToList();
            messagesForUser.showMessage(Level.INFO, _("Machine saved"));
        } catch (ValidationException e) {
            messagesForUser.showInvalidValues(e);
        }
    }

    public void saveAndContinue() {
        try {
            saveCalendar();
            saveCriterions();
            machineModel.confirmSave();
            goToEditForm(machineModel.getMachine());
            messagesForUser.showMessage(Level.INFO,_("Machine saved"));
        } catch (ValidationException e) {
            messagesForUser.showMessage(Level.ERROR,
                    _("Could not save machine") + " " + showInvalidValues(e));
        }
    }

    private String showInvalidValues(ValidationException e) {
        String result = "";
        for (InvalidValue each : e.getInvalidValues())
            result = result + each.getMessage();
        return result;
    }

    private void saveCalendar() throws ValidationException {
        if (baseCalendarEditionController != null) {
            baseCalendarEditionController.save();
        }
        if (machineModel.getCalendar() == null) {
            createCalendar();
        }
    }

    private void saveCriterions() throws ValidationException {
        if (criterionsController != null) {
            criterionsController.validate();
            criterionsController.save();
        }
    }

    private void goToList() {
        getVisibility().showOnly(listWindow);
        Util.reloadBindings(listWindow);
    }

    public void cancel() {
        goToList();
    }

    public List<BaseCalendar> getBaseCalendars() {
        return machineModel.getBaseCalendars();
    }

    private IBaseCalendarModel resourceCalendarModel;

    private void createCalendar() {
        Combobox combobox = (Combobox) editWindow
                .getFellow("createDerivedCalendar");
        Comboitem selectedItem = combobox.getSelectedItem();
        if (selectedItem == null) {
            throw new WrongValueException(combobox,
                    _("Please, select a calendar"));
        }

        BaseCalendar parentCalendar = (BaseCalendar) combobox.getSelectedItem()
                .getValue();
        if (parentCalendar == null) {
            parentCalendar = machineModel.getDefaultCalendar();
        }

        machineModel.setCalendar(parentCalendar.newDerivedResourceCalendar());
    }

    private Window editCalendarWindow;

    private Window createNewVersionWindow;

    private BaseCalendarEditionController baseCalendarEditionController;

    private void updateCalendarController() {
        editCalendarWindow = (Window) editWindow
                .getFellowIfAny("editCalendarWindow");
        createNewVersionWindow = (Window) editWindow
                .getFellowIfAny("createNewVersion");

        createNewVersionWindow.setVisible(true);
        createNewVersionWindow.setVisible(false);

        baseCalendarEditionController = new BaseCalendarEditionController(
                resourceCalendarModel, editCalendarWindow,
                createNewVersionWindow) {

            @Override
            public void goToList() {
                machineModel
                        .setCalendarOfMachine((ResourceCalendar) resourceCalendarModel
                                .getBaseCalendar());
                reloadWindow();
            }

            @Override
            public void cancel() {
                resourceCalendarModel.cancel();
                machineModel.setCalendarOfMachine(null);
                reloadWindow();
            }

            @Override
            public void save() {
                machineModel
                        .setCalendarOfMachine((ResourceCalendar) resourceCalendarModel
                                .getBaseCalendar());
                reloadWindow();
            }

        };

        editCalendarWindow.setVariable("calendarController", this, true);
        createNewVersionWindow.setVariable("calendarController", this, true);
    }

    private void reloadWindow() {
        Util.reloadBindings(editWindow);
    }

    public boolean isCalendarNull() {
        return (machineModel.getCalendarOfMachine() == null);
    }

    public boolean isCalendarNotNull() {
        return !isCalendarNull();
    }

    public BaseCalendarEditionController getEditionController() {
        return baseCalendarEditionController;
    }

    @SuppressWarnings("unused")
    private CriterionsController getCriterionsController() {
        return (CriterionsController) editWindow.getFellow(
                "criterionsContainer").getAttribute(
                "assignedCriterionsController");
    }

    public MachineConfigurationController getConfigurationController() {
        return configurationController;
    }

    public BaseCalendarsComboitemRenderer getBaseCalendarsComboitemRenderer() {
        return baseCalendarsComboitemRenderer;
    }

    private class BaseCalendarsComboitemRenderer implements ComboitemRenderer {

        @Override
        public void render(Comboitem item, Object data) throws Exception {
            BaseCalendar calendar = (BaseCalendar) data;
            item.setLabel(calendar.getName());
            item.setValue(calendar);

            if (isDefaultCalendar(calendar)) {
                Combobox combobox = (Combobox) item.getParent();
                combobox.setSelectedItem(item);
            }
        }

        private boolean isDefaultCalendar(BaseCalendar calendar) {
            BaseCalendar defaultCalendar = machineModel.getDefaultCalendar();
            return defaultCalendar.getId().equals(calendar.getId());
        }

    }

}