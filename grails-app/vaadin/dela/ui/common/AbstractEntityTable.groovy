package dela.ui.common

import com.vaadin.data.Container
import com.vaadin.data.util.BeanItem
import com.vaadin.event.ItemClickEvent
import com.vaadin.event.ShortcutAction
import com.vaadin.event.ShortcutListener
import com.vaadin.terminal.FileResource
import com.vaadin.ui.Button
import com.vaadin.ui.Button.ClickEvent
import com.vaadin.ui.Button.ClickListener
import com.vaadin.ui.Form
import com.vaadin.ui.FormFieldFactory
import com.vaadin.ui.HorizontalLayout
import com.vaadin.ui.Table
import com.vaadin.ui.VerticalLayout
import com.vaadin.ui.Window
import dela.YesNoDialog
import dela.VaadinService
import dela.IDataService
import dela.CommonDataService
import dela.context.DataContext

/**
 * @author vedi
 * date 28.06.2010
 * time 18:11:29
 */
public abstract class AbstractEntityTable extends VerticalLayout implements ClickListener {

    VaadinService vaadinService
    IDataService dataService

    def dataContext

    Table table

    HorizontalLayout toolBarLayout

    Button addButton
    Button editButton
    Button deleteButton
    Button refreshButton

    protected Container container

    def formFieldFactory

    def saveHandler = {item ->
        this.saveItem(item)
    }

    def AbstractEntityTable() {
        this.dataService = initDataService()
        this.vaadinService = getBean(VaadinService.class)
        this.table = new Table()
    }

    def setDropHandler(dropHandler) {
        table.dropHandler = dropHandler
    }

    @Override
    public void attach() {

        assert dataContext

        table.addShortcutListener(new ShortcutListener("down", ShortcutAction.KeyCode.ARROW_DOWN, new int[0]) {
            void handleAction(Object o, Object o1) {
                AbstractEntityTable.this.table.select(AbstractEntityTable.this.table.nextItemId(AbstractEntityTable.this.table.getValue()))
            }
        });
        table.addShortcutListener(new ShortcutListener("up", ShortcutAction.KeyCode.ARROW_UP, new int[0]) {
            void handleAction(Object o, Object o1) {
                AbstractEntityTable.this.table.select(AbstractEntityTable.this.table.prevItemId(AbstractEntityTable.this.table.getValue()))
            }
        });

        toolBarLayout = new HorizontalLayout();
        initToolBar(toolBarLayout)

        this.addComponent toolBarLayout

        initTable()

        super.attach()

    }

    protected IDataService initDataService() {
        return getBean(CommonDataService.class)
    }

    protected void initToolBar(toolBar) {

        addButton = new Button();
        addButton.setDescription(i18n('button.create.label', 'create'))
        addButton.setClickShortcut(ShortcutAction.KeyCode.INSERT)
        addButton.setIcon(new FileResource(vaadinService.getFile('images/skin/database_add.png'), this.window.application))
        addButton.addListener(this as ClickListener)
        toolBar.addComponent addButton

        editButton = new Button();
        editButton.setDescription(i18n('button.edit.label', 'edit'))
        editButton.setClickShortcut(ShortcutAction.KeyCode.ENTER)
        editButton.setIcon(new FileResource(vaadinService.getFile('images/skin/database_edit.png'), this.window.application))
        editButton.addListener(this as ClickListener)
        toolBar.addComponent editButton

        deleteButton = new Button();
        deleteButton.setDescription(i18n('button.delete.label', 'delete'))
        deleteButton.setClickShortcut(ShortcutAction.KeyCode.DELETE)
        deleteButton.setIcon(new FileResource(vaadinService.getFile('images/skin/database_delete.png'), this.window.application))
        deleteButton.addListener(this as ClickListener)
        toolBar.addComponent deleteButton

        refreshButton = new Button();
        refreshButton.setDescription(i18n('button.refresh.label', 'refresh'))
        refreshButton.setIcon(new FileResource(vaadinService.getFile('images/skin/database_refresh.png'), this.window.application))
        refreshButton.addListener(this as ClickListener)
        toolBar.addComponent refreshButton
    }

    protected void initTable() {

        this.table.immediate = true
        this.table.selectable = true
        this.table.nullSelectionAllowed = false

        this.caption = vaadinService.getMetaListCaption(dataContext)

        this.table.addListener(new ItemClickEvent.ItemClickListener() {
            public void itemClick(ItemClickEvent event) {
                if (event.doubleClick) {
                    showForm(event.item, canEdit(getDomain(event.item)))
                }
            }
        })

        container = createContainer(dataContext)

        this.table.setContainerDataSource(this.container)

        this.table.setVisibleColumns(getGridVisibleColumns() as Object[]);
        this.table.setColumnHeaders(getColumnHeaders())

        initGrid();

        this.table.setSizeFull()

        this.addComponent(this.table)
        this.setExpandRatio(this.table, 1.0f)
    }

    abstract protected Container createContainer(DataContext dataContext)

    abstract protected void refreshContainer()

    def initGrid() {
    }

    public void refresh() {
        refreshContainer()
    }

    void buttonClick(ClickEvent clickEvent) {
        if (clickEvent.button == addButton) {
            if (canInsert()) {
                showForm(new BeanItem(createDomain()))
            }
        } else if (clickEvent.button == editButton) {
            if (table.value != null) {
                def item = container.getItem(table.value)
                if (item) {
                    showForm(item, canEdit(getDomain(item)))
                }
            }
        } else if (clickEvent.button == deleteButton) {
            if (table.value != null) {
                def item = container.getItem(table.value)
                if (item) {
                    def domain = getDomain(item)
                    if (canDelete(domain)) {
                        remove(domain)
                    }
                }
            }
        } else if (clickEvent.button == refreshButton) {
            refresh()
        }
    }

    void remove(domain) {
        this.window.application.mainWindow.addWindow(new YesNoDialog(
                i18n('delete.confirm.caption', 'confirm delete'),
                i18n('delete.confirm.message', 'are you sure?'),
                i18n('button.yes.label', 'yes'),
                i18n('button.no.label', 'no'),
                new YesNoDialog.Callback() {
                    public void onDialogResult(boolean yes) {
                        if (yes) {
                            AbstractEntityTable.this.doRemove(domain)
                        }
                    }

                }))
    }

    protected void doRemove(domain) {
        dataService.delete(dataContext, domain)
        refresh()
    }

    void showForm(selectedItem, editable = true) {
        Window window = new Window(vaadinService.getMetaCaption(dataContext))

        Form form = createForm()
        form.editable = editable

        if (getFormFieldFactory()) {
            form.formFieldFactory = getFormFieldFactory() as FormFieldFactory
        }

        form.setItemDataSource(selectedItem, getEditVisibleColumns())
        form.saveHandler = saveHandler

        window.addComponent(form)

        window.layout.setSizeUndefined()
        window.center()

        this.window.application.mainWindow.addWindow window
    }

    protected List<String> getGridVisibleColumns() {
        return vaadinService.getGridVisibleColumns(dataContext)
    }

    protected List<String> getEditVisibleColumns() {
        return vaadinService.getEditVisibleColumns(dataContext)
    }

    protected Form createForm() {
        return new EntityForm()
    }

    protected createDomain() {
        return dataService.create(dataContext)
    }

    protected String[] getColumnHeaders() {
        return getGridVisibleColumns().collect {getColumnLabel(it)} as String[]
    }

    protected select(Long id) {
        def itemIds = this.container.getItemIds()
        for (itemId in itemIds) {
            if (this.container.getItem(itemId).getItemProperty('id').value.equals(id)) {
                this.table.setValue(itemId)
                break
            }
        }
    }

    def getColumnLabel(columnName) {
        vaadinService.getColumnCaption(dataContext, columnName)
    }

    def canInsert() {
        return dataService.canInsert(dataContext)
    }

    def canEdit(domain) {
        return dataService.canEdit(dataContext, domain)
    }

    def canDelete(domain) {
        return dataService.canDelete(dataContext, domain)
    }

    def afterInsert(item) {
        this.select(getDomain(item).id)
    }

    def afterEdit(item) {
    }

    /**
     * Save item to db.
     * @param item item to save
     * @return result domain
     */
    private def saveItem(item) {
        def domain = getDomain(item)

        boolean isNew = domain.id == null

        domain = dataService.save(dataContext, domain)

        this.refresh()
        if (isNew) {
            afterInsert(item)
        } else {
            afterEdit(item)
        }

        domain
    }

    final protected def getDomain(item) {
        (item as BeanItem).bean
    }
}
