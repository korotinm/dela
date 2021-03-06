package dela.ui.account

import com.vaadin.data.Item
import com.vaadin.data.util.BeanItem
import com.vaadin.ui.Button
import com.vaadin.ui.Button.ClickEvent
import com.vaadin.ui.Button.ClickListener
import com.vaadin.ui.Component
import com.vaadin.ui.ComponentContainer
import com.vaadin.ui.Field
import com.vaadin.ui.FormFieldFactory
import com.vaadin.ui.TextField
import com.vaadin.ui.Window
import dela.Account
import dela.ui.common.EntityForm

/**
 * @author vedi
 * date 20.07.2010
 * time 8:34:32
 */
class LoginWindow extends Window implements FormFieldFactory, ClickListener {

    def form
    Account account

    TextField loginField

    def loginCallback
    def forgetPasswordCallback

    def login = {item ->
        loginCallback(account.login, account.password)
    }

    def LoginWindow(loginCallback, forgetPasswordCallback) {

        this.loginCallback = loginCallback
        this.forgetPasswordCallback = forgetPasswordCallback
        this.caption = i18n("window.login.caption", "login")

        form = new EntityForm() {

            protected void initButtons(ComponentContainer componentContainer) {
                super.initButtons(componentContainer)

                Button button = new Button()
                button.caption = i18n("window.login.forgetPassword.label", "forgetPassword")
                button.addListener(LoginWindow.this as ClickListener)
                componentContainer.addComponent button
            }
        }

        account = new Account()

        form.editable = true
        form.formFieldFactory = this
        form.itemDataSource = new BeanItem(account)
        form.visibleItemProperties = ['login', 'password']

        form.saveHandler = login

        this.addComponent(form)

        this.modal = true;

        this.layout.setSizeUndefined()
        this.center()
    }

    def void attach() {
        super.attach();
        loginField.focus()
    }

    Field createField(Item item, Object propertyId, Component uiContext) {
        String label = i18n("entity.account.field.${propertyId}.label", propertyId)
        if (propertyId == 'login') {
            loginField = new TextField(label)
            loginField.setNullRepresentation('')
            return loginField
        } else if (propertyId == 'password') {
            TextField textField = new TextField(label)
            textField.secret = true
            textField.setNullRepresentation('')
            return textField
        } else {
            return null
        }
    }

    void buttonClick(ClickEvent event) {
        forgetPasswordCallback()                       

        window.application.mainWindow.removeWindow this
    }
}
