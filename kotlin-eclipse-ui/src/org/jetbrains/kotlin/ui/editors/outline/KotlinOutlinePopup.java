package org.jetbrains.kotlin.ui.editors.outline;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.utils.EditorUtil;

import com.intellij.psi.PsiElement;

public class KotlinOutlinePopup extends PopupDialog implements IInformationControl, IInformationControlExtension, IInformationControlExtension2, DisposeListener {
    
    private TreeViewer treeViewer;
    private final JavaEditor editor;
    private final int treeStyle;
    private Text filterInputText;
    
    public KotlinOutlinePopup(JavaEditor editor, Shell parent, int shellStyle, int treeStyle) {
        this(editor, parent, shellStyle, treeStyle, true, true, false, true, true, null, null);
    }

    public KotlinOutlinePopup(JavaEditor editor, Shell parent, int shellStyle, int treeStyle, boolean takeFocusOnOpen, boolean persistSize,
            boolean persistLocation, boolean showDialogMenu, boolean showPersistActions, String titleText, String infoText) {
        super(parent, shellStyle, takeFocusOnOpen, persistSize, persistLocation, showDialogMenu, showPersistActions, titleText, infoText);
        
        this.editor = editor;
        this.treeStyle = treeStyle;
        
        create();
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        treeViewer = createTreeViewer(parent, treeStyle);

        Tree tree = treeViewer.getTree();
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e)  {
                if (e.character == SWT.ESC) { 
                    dispose();
                }
            }
        });

        tree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                gotoSelectedElement();
            }
        });
        
        installInputFilter();
        
        return treeViewer.getControl();
    }
    
    protected Text createFilterText(Composite parent) {
        filterInputText = new Text(parent, SWT.NONE);

        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.CENTER;
        filterInputText.setLayoutData(data);

        filterInputText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.keyCode) {
                    case SWT.CR: case SWT.LF:
                        gotoSelectedElement();
                        break;
                    case SWT.ARROW_DOWN: case SWT.ARROW_UP:
                        treeViewer.getTree().setFocus();
                        break;
                    case SWT.ESC:
                        dispose();
                        break;
                }
            }
        });

        return filterInputText;
    }
    
    private void installInputFilter() {
        filterInputText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                selectFirstMatch();
            }
        });
    }
    
    @Override
    protected Control createTitleMenuArea(Composite parent) {
        filterInputText = createFilterText(parent);
        filterInputText.setFocus();

        return null;
    }
    
    private void selectFirstMatch() {
        Tree tree = treeViewer.getTree();
        for (TreeItem item : tree.getItems()) {
            if (selectFirstMatch(item, filterInputText.getText())) {
                break;
            }
        }
    }
    
    private boolean selectFirstMatch(TreeItem item, String text) {
        if (item.getText().toLowerCase().startsWith(text.toLowerCase())) {
            treeViewer.getTree().select(item);
            treeViewer.getTree().showItem(item);
            return true;
        }
        
        for (TreeItem childItem : item.getItems()) {
            selectFirstMatch(childItem, text);
        }
        
        return false;
    }
    
    protected TreeViewer createTreeViewer(Composite parent, int style) {
        Tree tree = new Tree(parent, SWT.SINGLE | style);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = tree.getItemHeight() * 14;
        tree.setLayoutData(gridData);
        
        treeViewer = new TreeViewer(tree);
        treeViewer.setLabelProvider(new PsiLabelProvider());
        treeViewer.setContentProvider(new PsiContentProvider());
        
        return treeViewer;
    }
    
    private void gotoSelectedElement() {
        Object treeSelection = ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
        if (treeSelection instanceof PsiElement) {
            PsiElement psiElement = (PsiElement) treeSelection;
            int offset = EditorUtil.getOffsetInEditor(editor, psiElement.getTextOffset());
             
            editor.selectAndReveal(offset, 0);
            close();
       }
    }

    @Override
    public void widgetDisposed(DisposeEvent e) {
        filterInputText = null;
        treeViewer = null;
    }

    @Override
    public void setInput(Object input) {
        PsiElement element = KotlinPsiManager.INSTANCE.getParsedFile(EditorUtil.getFile(editor));
        treeViewer.setInput(element);
    }

    @Override
    public boolean hasContents() {
        return treeViewer != null && treeViewer.getInput() != null;
    }

    @Override
    public void setInformation(String information) {
    }

    @Override
    public void setSizeConstraints(int maxWidth, int maxHeight) {
    }

    @Override
    public Point computeSizeHint() {
        return getShell().getSize();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            open();
        } else {
            getShell().setVisible(false);
        }
    }

    @Override
    public void setSize(int width, int height) {
        getShell().setSize(width, height);
    }

    @Override
    public void setLocation(Point location) {
        if (!getPersistLocation() || getDialogSettings() == null)
            getShell().setLocation(location);
    }

    @Override
    public void dispose() {
        close();
    }

    @Override
    public void addDisposeListener(DisposeListener listener) {
        getShell().addDisposeListener(listener);
    }

    @Override
    public void removeDisposeListener(DisposeListener listener) {
        getShell().removeDisposeListener(listener);
    }

    @Override
    public void setForegroundColor(Color foreground) {
        applyForegroundColor(foreground, getContents());
    }

    @Override
    public void setBackgroundColor(Color background) {
        applyBackgroundColor(background, getContents());
    }

    @Override
    public boolean isFocusControl() {
        return getShell().getDisplay().getActiveShell() == getShell();
    }

    @Override
    public void setFocus() {
        treeViewer.refresh();
        treeViewer.expandAll();
        filterInputText.forceFocus();
    }

    @Override
    public void addFocusListener(FocusListener listener) {
        getShell().addFocusListener(listener);
    }

    @Override
    public void removeFocusListener(FocusListener listener) {
        getShell().removeFocusListener(listener);
    }
}
