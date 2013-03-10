package org.jetbrains.kotlin.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class PackageSelectionDialog extends ElementListSelectionDialog {

    IPackageFragmentRoot sourceDir;
    
    public PackageSelectionDialog(Shell parent, //IProject project, 
            IPackageFragmentRoot sourceDir) {
        super(parent, new ILabelProvider() {
            @Override
            public void removeListener(ILabelProviderListener listener) {}
            @Override
            public boolean isLabelProperty(Object element, String property) {
                return false;
            }
            @Override
            public void dispose() {}
            @Override
            public void addListener(ILabelProviderListener listener) {}
            @Override
            public String getText(Object element) {
                String name = ((IPackageFragment) element).getElementName();
                if (name.isEmpty()) {
                    return "default package";
                }
                else {
                    return name;
                }
            }
            @Override
            public Image getImage(Object element) {
                return null;
            }
        });
        this.sourceDir = sourceDir;
    }
    
    @Override
    public int open() {
        List<IPackageFragment> elements = new ArrayList<IPackageFragment>();
        try {
            addChildren(elements, sourceDir.getChildren());
        }
        catch (JavaModelException jme) {
            jme.printStackTrace();
        }
        /*Collections.sort(elements, new Comparator<IPackageFragment>() {
            @Override
            public int compare(IPackageFragment pf1, IPackageFragment pf2) {
                return pf1.getElementName().compareTo(pf2.getElementName());
            }
        });*/
        setElements(elements.toArray());
        return super.open();
    }

    public void addChildren(List<IPackageFragment> elements, IJavaElement[] children)
            throws JavaModelException {
        for (IJavaElement je: children) {
            if (je instanceof IPackageFragment) {
                IPackageFragment pf = (IPackageFragment) je;
                elements.add(pf);
                addChildren(elements, pf.getChildren());
            }
        }
    }
    
}
