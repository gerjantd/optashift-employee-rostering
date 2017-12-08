package org.optaplanner.openshift.employeerostering.gwtui.client.employee;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import elemental2.dom.CanvasRenderingContext2D;
import elemental2.dom.MouseEvent;
import org.gwtbootstrap3.client.ui.html.Div;
import org.optaplanner.openshift.employeerostering.gwtui.client.calendar.AbstractDrawable;
import org.optaplanner.openshift.employeerostering.gwtui.client.calendar.TimeRowDrawable;
import org.optaplanner.openshift.employeerostering.gwtui.client.calendar.TwoDayView;
import org.optaplanner.openshift.employeerostering.gwtui.client.canvas.CanvasUtils;
import org.optaplanner.openshift.employeerostering.gwtui.client.canvas.ColorUtils;
import org.optaplanner.openshift.employeerostering.gwtui.client.common.CommonUtils;
import org.optaplanner.openshift.employeerostering.gwtui.client.common.FailureShownRestCallback;
import org.optaplanner.openshift.employeerostering.gwtui.client.css.CssParser;
import org.optaplanner.openshift.employeerostering.gwtui.client.popups.ErrorPopup;
import org.optaplanner.openshift.employeerostering.gwtui.client.popups.FormPopup;
import org.optaplanner.openshift.employeerostering.gwtui.client.resources.css.CssResources;
import org.optaplanner.openshift.employeerostering.gwtui.client.resources.i18n.OptaShiftUIConstants;
import org.optaplanner.openshift.employeerostering.shared.employee.EmployeeAvailability;
import org.optaplanner.openshift.employeerostering.shared.employee.EmployeeRestServiceBuilder;
import org.optaplanner.openshift.employeerostering.shared.employee.view.EmployeeAvailabilityView;
import org.optaplanner.openshift.employeerostering.shared.employee.EmployeeAvailabilityState;
import org.optaplanner.openshift.employeerostering.shared.shift.Shift;
import org.optaplanner.openshift.employeerostering.shared.shift.ShiftRestServiceBuilder;
import org.optaplanner.openshift.employeerostering.shared.shift.view.ShiftView;
import org.optaplanner.openshift.employeerostering.shared.spot.Spot;
import org.optaplanner.openshift.employeerostering.shared.spot.SpotRestServiceBuilder;

public class EmployeeDrawable extends AbstractDrawable implements TimeRowDrawable<EmployeeId> {

    TwoDayView<EmployeeId, ?, ?> view;
    EmployeeData data;
    int index;
    boolean isMouseOver;

    public EmployeeDrawable(TwoDayView<EmployeeId, ?, ?> view, EmployeeData data, int index) {
        this.view = view;
        this.data = data;
        this.index = index;
        this.isMouseOver = false;
        //ErrorPopup.show(this.toString());
    }

    @Override
    public double getLocalX() {
        double start = getStartTime().toEpochSecond(ZoneOffset.UTC) / 60;
        return start * view.getWidthPerMinute();
    }

    @Override
    public double getLocalY() {
        Integer cursorIndex = view.getCursorIndex(getGroupId());
        return (null != cursorIndex && cursorIndex > index) ? index * view.getGroupHeight() : (index + 1) * view
                .getGroupHeight();
    }

    @Override
    public void doDrawAt(CanvasRenderingContext2D g, double x, double y) {
        String color = (isMouseOver) ? ColorUtils.brighten(getFillColor()) : getFillColor();
        CanvasUtils.setFillColor(g, color);

        double start = getStartTime().toEpochSecond(ZoneOffset.UTC) / 60;
        double end = getEndTime().toEpochSecond(ZoneOffset.UTC) / 60;
        double duration = end - start;

        CanvasUtils.drawCurvedRect(g, x, y, duration * view.getWidthPerMinute(), view.getGroupHeight());

        CanvasUtils.setFillColor(g, ColorUtils.getTextColor(color));

        String spot;
        if (null == data.getSpot()) {
            spot = "Unassigned";
        } else {
            spot = data.getSpot().getName();
        }
        String pad = (data.isLocked()) ? "BB" : "";

        int fontSize = CanvasUtils.fitTextToBox(g, spot + pad, duration * view.getWidthPerMinute() * 0.75, view
                .getGroupHeight() * 0.75);
        g.font = CanvasUtils.getFont(fontSize);
        double[] textSize = CanvasUtils.getPreferredBoxSizeForText(g, spot, 12);

        g.fillText(spot, x + (duration * view.getWidthPerMinute() - textSize[0]) * 0.5,
                y + (view.getGroupHeight() + textSize[1]) * 0.5);

        if (data.isLocked()) {
            CanvasUtils.drawGlyph(g, CanvasUtils.Glyphs.LOCK, fontSize, x +
                    (duration * view.getWidthPerMinute() + textSize[0]) * 0.5, y + (view.getGroupHeight() + textSize[1])
                            * 0.5);
        }
    }

    @Override
    public boolean onMouseMove(MouseEvent e, double x, double y) {
        view.preparePopup(this.toString());
        return true;
    }

    @Override
    public PostMouseDownEvent onMouseDown(MouseEvent mouseEvent, double x, double y) {
        SpotRestServiceBuilder.getSpotList(data.getShift().getTenantId(), new FailureShownRestCallback<List<Spot>>() {

            @Override
            public void onSuccess(List<Spot> spotList) {
                //TODO: i18n
                FormPopup popup = FormPopup.getFormPopup();

                VerticalPanel panel = new VerticalPanel();
                panel.setStyleName(FormPopup.getStyles().form());
                HorizontalPanel datafield = new HorizontalPanel();

                Label label = new Label("Is Locked");
                CheckBox checkbox = new CheckBox();
                checkbox.setValue(data.isLocked());
                datafield.add(label);
                datafield.add(checkbox);
                panel.add(datafield);

                datafield = new HorizontalPanel();
                label = new Label("Assigned Spot");
                ListBox assignedSpot = new ListBox();
                spotList.forEach((s) -> assignedSpot.addItem(s.getName()));
                if (!data.isLocked()) {
                    assignedSpot.setEnabled(false);
                } else {
                    assignedSpot.setSelectedIndex(spotList.indexOf(data.getSpot()));
                }
                checkbox.addValueChangeHandler((v) -> assignedSpot.setEnabled(v.getValue()));
                datafield.add(label);
                datafield.add(assignedSpot);
                panel.add(datafield);

                datafield = new HorizontalPanel();
                label = new Label("Avaliability");
                ListBox employeeAvaliability = new ListBox();
                int index = 0;
                for (EmployeeAvailabilityState availabilityState : EmployeeAvailabilityState.values()) {
                    employeeAvaliability.addItem(availabilityState.toString());
                    if (null != data.getAvailability() && availabilityState.equals(data.getAvailability().getState())) {
                        employeeAvaliability.setSelectedIndex(index);
                    }
                    index++;
                }
                employeeAvaliability.addItem("NO PREFERENCE");
                if (null == data.getAvailability()) {
                    employeeAvaliability.setSelectedIndex(index);
                }
                datafield.add(label);
                datafield.add(employeeAvaliability);
                panel.add(datafield);

                datafield = new HorizontalPanel();
                Button confirm = new Button();
                confirm.setText(view.getTranslator().format(OptaShiftUIConstants.General_confirm));
                confirm.setStyleName(FormPopup.getStyles().submit());
                confirm.addClickHandler((c) -> {
                    EmployeeAvailabilityState state = null;
                    try {
                        state = EmployeeAvailabilityState.valueOf(employeeAvaliability.getSelectedValue());
                        if (null == data.getAvailability()) {
                            EmployeeAvailabilityView availabilityView = new EmployeeAvailabilityView(data.getShift()
                                    .getTenantId(), data.getEmployee(), data.getShift().getTimeSlot(), state);
                            EmployeeRestServiceBuilder.addEmployeeAvailability(data.getShift().getTenantId(),
                                    availabilityView, new FailureShownRestCallback<Long>() {

                                        @Override
                                        public void onSuccess(Long id) {
                                            view.getCalendar().forceUpdate();
                                        }
                                    });
                        } else {
                            data.getAvailability().setState(state);
                            EmployeeRestServiceBuilder.updateEmployeeAvailability(data.getAvailability().getTenantId(),
                                    data.getAvailability(), new FailureShownRestCallback<Void>() {

                                        @Override
                                        public void onSuccess(Void result) {
                                            view.getCalendar().forceUpdate();
                                        }
                                    });
                        }
                    } catch (IllegalArgumentException e) {
                        if (data.getAvailability() != null) {
                            EmployeeRestServiceBuilder.removeEmployeeAvailability(data.getAvailability().getTenantId(),
                                    data.getAvailability().getId(), new FailureShownRestCallback<Boolean>() {

                                        @Override
                                        public void onSuccess(Boolean result) {
                                            view.getCalendar().forceUpdate();
                                        }
                                    });
                        }
                    }

                    if (checkbox.getValue()) {
                        Spot spot = spotList.stream().filter((e) -> e.getName().equals(assignedSpot.getSelectedValue()))
                                .findFirst().get();
                        popup.hide();
                        ShiftRestServiceBuilder.getShifts(spot.getTenantId(), new FailureShownRestCallback<List<
                                ShiftView>>() {

                            @Override
                            public void onSuccess(List<ShiftView> shifts) {
                                ShiftView shift = shifts.stream().filter((s) -> s.getSpotId().equals(spot.getId()) && s
                                        .getTimeSlotId().equals(data.getShift().getTimeSlot().getId())).findFirst()
                                        .orElseGet(() -> null);
                                if (null != shift) {
                                    data.getShift().setLockedByUser(false);
                                    shift.setEmployeeId(data.getEmployee().getId());
                                    shift.setLockedByUser(true);
                                    if (data.isLocked()) {
                                        ShiftView oldShift = new ShiftView(data.getShift());

                                        ShiftRestServiceBuilder.updateShift(data.getShift().getTenantId(), oldShift,
                                                new FailureShownRestCallback<Void>() {

                                                    @Override
                                                    public void onSuccess(Void result) {
                                                        ShiftRestServiceBuilder.updateShift(data.getShift()
                                                                .getTenantId(),
                                                                shift, new FailureShownRestCallback<Void>() {

                                                                    @Override
                                                                    public void onSuccess(Void result2) {
                                                                        view.getCalendar().forceUpdate();
                                                                    }

                                                                });
                                                    }

                                                });
                                    } else {
                                        ShiftRestServiceBuilder.updateShift(data.getShift().getTenantId(), shift,
                                                new FailureShownRestCallback<Void>() {

                                                    @Override
                                                    public void onSuccess(Void result) {
                                                        view.getCalendar().forceUpdate();
                                                    }
                                                });
                                    }

                                } else {
                                    ErrorPopup.show("Cannot find shift with spot " + spot.getName() + " for timeslot "
                                            + data.getShift().getTimeSlot());
                                }
                            }
                        });
                    } else if (data.isLocked()) {
                        data.getShift().setLockedByUser(false);
                        ShiftView shiftView = new ShiftView(data.getShift());
                        popup.hide();
                        ShiftRestServiceBuilder.updateShift(data.getShift().getTenantId(), shiftView,
                                new FailureShownRestCallback<Void>() {

                                    @Override
                                    public void onSuccess(Void result) {
                                        view.getCalendar().forceUpdate();
                                    }

                                });
                    } else {
                        popup.hide();
                    }

                });

                Button cancel = new Button();
                // TODO: Replace with i18n later
                cancel.setText("Cancel");
                cancel.setStyleName(FormPopup.getStyles().cancel());
                cancel.addClickHandler((e) -> popup.hide());

                Div submitDiv = new Div();
                submitDiv.setStyleName(FormPopup.getStyles().submitDiv());

                datafield.setStyleName(FormPopup.getStyles().buttonGroup());
                datafield.add(cancel);
                datafield.add(confirm);

                submitDiv.add(datafield);
                panel.add(submitDiv);

                popup.setWidget(panel);
                popup.center();
            }
        });

        return PostMouseDownEvent.REMOVE_FOCUS;
    }

    @Override
    public boolean onMouseEnter(MouseEvent e, double x, double y) {
        isMouseOver = true;
        return true;
    }

    @Override
    public boolean onMouseExit(MouseEvent e, double x, double y) {
        isMouseOver = false;
        return true;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public EmployeeId getGroupId() {
        return data.getGroupId();
    }

    @Override
    public LocalDateTime getStartTime() {
        return data.getStartTime();
    }

    @Override
    public LocalDateTime getEndTime() {
        return data.getEndTime();
    }

    @Override
    public void doDraw(CanvasRenderingContext2D g) {
        doDrawAt(g, getGlobalX(), getGlobalY());
    }

    public String toString() {
        StringBuilder out = new StringBuilder(data.getEmployee().getName());
        out.append(' ');
        out.append(CommonUtils.pad(getStartTime().getHour() + "", 2));
        out.append(':');
        out.append(CommonUtils.pad(getStartTime().getMinute() + "", 2));
        out.append('-');
        out.append(CommonUtils.pad(getEndTime().getHour() + "", 2));
        out.append(':');
        out.append(CommonUtils.pad(getEndTime().getMinute() + "", 2));
        out.append(" -- ");
        String spot;
        if (null == data.getSpot()) {
            spot = "Unassigned";
        } else {
            spot = data.getSpot().getName();
            if (data.isLocked()) {
                spot += " (locked)";
            }
        }
        out.append("Assigned to ");
        out.append(spot);
        out.append("; slot is ");
        out.append((data.getAvailability() != null) ? data.getAvailability().getState().toString() : "Indifferent");
        return out.toString();
    }

    private String getFillColor() {
        if (null == data.getAvailability()) {
            return CssParser.getCssProperty(CssResources.INSTANCE.calendar(),
                    CssResources.INSTANCE.calendar().employeeShiftViewIndifferent(),
                    "background-color");
        }

        switch (data.getAvailability().getState()) {
            case UNDESIRED:
                return CssParser.getCssProperty(CssResources.INSTANCE.calendar(),
                        CssResources.INSTANCE.calendar().employeeShiftViewUndesired(),
                        "background-color");
            case DESIRED:
                return CssParser.getCssProperty(CssResources.INSTANCE.calendar(),
                        CssResources.INSTANCE.calendar().employeeShiftViewDesired(),
                        "background-color");
            case UNAVAILABLE:
                return CssParser.getCssProperty(CssResources.INSTANCE.calendar(),
                        CssResources.INSTANCE.calendar().employeeShiftViewUnavailable(),
                        "background-color");
            default:
                return CssParser.getCssProperty(CssResources.INSTANCE.calendar(),
                        CssResources.INSTANCE.calendar().employeeShiftViewIndifferent(),
                        "background-color");
        }
    }

}
