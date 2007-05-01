package org.jboss.ide.eclipse.as.ui.packages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.internal.ServerPlugin;
import org.jboss.ide.eclipse.archives.core.model.ArchivesCore;
import org.jboss.ide.eclipse.archives.core.model.IArchive;
import org.jboss.ide.eclipse.archives.core.model.IArchiveFolder;
import org.jboss.ide.eclipse.archives.core.model.IArchiveType;
import org.jboss.ide.eclipse.archives.ui.ArchivesSharedImages;
import org.jboss.ide.eclipse.archives.ui.providers.ArchivesContentProvider;
import org.jboss.ide.eclipse.archives.ui.providers.ArchivesLabelProvider;
import org.jboss.ide.eclipse.archives.ui.wizards.AbstractArchiveWizard;
import org.jboss.ide.eclipse.archives.ui.wizards.WizardPageWithNotification;
import org.jboss.ide.eclipse.as.core.packages.types.WarArchiveType;

public abstract class PreviewPage extends WizardPageWithNotification {

	private Group previewGroup;
	protected AbstractArchiveWizard wizard;
	private TreeViewer previewViewer;
	private boolean hasCreated = false;
	public PreviewPage (AbstractArchiveWizard wizard, String name, String title, ImageDescriptor descriptor ) {
		super( name, title, descriptor);
		this.wizard = wizard;
	}
	
	protected abstract String getDescriptionMessage();
	public void createControl(Composite parent) {
		
		setMessage(getDescriptionMessage());
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new FormLayout());
		
		layoutGroups(main);
		fillGroups();
		
		setControl(main);
	}
	protected void layoutGroups(Composite main) {
		previewGroup = new Group(main, SWT.NONE);
		previewGroup.setText("Preview");
		FormData previewData = new FormData();
		previewData.left = new FormAttachment(0,5);
		previewData.right = new FormAttachment(100,-5);
		previewData.top = new FormAttachment(0,5);
		previewData.bottom = new FormAttachment(100,-5);
		previewGroup.setLayoutData(previewData);
		previewGroup.setLayout(new FormLayout());
		previewViewer = new TreeViewer(previewGroup);
		previewViewer.setLabelProvider(new ArchivesLabelProvider());
		previewViewer.setContentProvider(new ArchivesContentProvider());
		FormData warPreviewData = new FormData();
		warPreviewData.left = new FormAttachment(0,5);
		warPreviewData.right = new FormAttachment(100,-5);
		warPreviewData.top = new FormAttachment(0,5);
		warPreviewData.bottom = new FormAttachment(100,-5);
		previewViewer.getTree().setLayoutData(warPreviewData);
		
	}

	protected void fillGroups() {
	}
	public boolean isPageComplete() {
		return hasCreated;
	}
    public void pageEntered(int button) {
    	if( !hasCreated ) {
    		addToPackage();
    		hasCreated = true;
    	}
    	fillWidgets(wizard.getArchive());
    	
    	// if it's already a module type project, hide the meta inf stuff
		IModuleArtifact moduleArtifact = ServerPlugin.loadModuleArtifact(wizard.getProject());
		if( moduleArtifact.getModule() != null ) {
			FormData d = (FormData)previewGroup.getLayoutData();
			d.top = new FormAttachment(0,5);
			previewGroup.setLayoutData(d);
			((Composite)getControl()).layout();
		}
		getWizard().getContainer().updateButtons();
    }
    
    protected abstract void addToPackage();
    protected void fillWidgets(IArchive pkg) {
    	previewViewer.setInput(new IArchive[] {pkg});
    	previewViewer.expandAll();
    }
    
    protected IArchiveFolder getFolder(IArchive pkg, String folderName) {
    	IArchiveFolder result = null;
    	IArchiveFolder[] folders = pkg.getFolders();
    	for( int i = 0; i < folders.length; i++ ) {
    		if( folders[i].getName().equals(folderName)) {
    			result = folders[i];
    			break;
    		}
    	}
    	return result;
    }
    
    public void pageExited(int button) {}

    
    
    
    // stuff that can be extracted
    public static class WorkspaceFolderSelectionDialog extends ElementTreeSelectionDialog {
    	
    public WorkspaceFolderSelectionDialog(Shell parent, boolean allowMultiple, String selectedPaths) {
   		 super(parent, new FolderLabelProvider(), new FolderContentProvider());
   		 setAllowMultiple(allowMultiple);
   		 setupDestinationList();
   		 setupInitialSelections(selectedPaths);
   	}
   	 
   	 private void setupDestinationList () {
   		 List projects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
   		 setInput(projects);
   	 }
   	 private void setupInitialSelections(String initialSelection) {
   		 ArrayList resources = new ArrayList();
   		 String[] paths = initialSelection.split(",");
   		 // find IResources
   		 IResource res;
   		 for( int i = 0; i < paths.length; i++ ) {
   			 res = ResourcesPlugin.getWorkspace().getRoot().findMember(paths[i]);
   			 resources.add(res);
   		 }
   		 setInitialSelections((IResource[]) resources.toArray(new IResource[resources.size()]));
   	 }
   	 
   	 private static class FolderContentProvider implements ITreeContentProvider {
   		private static final Object[] NO_CHILDREN = new Object[0];
   		public Object[] getChildren(Object parentElement) {
   			if (parentElement instanceof IContainer) {
   				IContainer container = (IContainer) parentElement;
   				try {
   					IResource members[] = container.members();
   					List folders = new ArrayList();
   					for (int i = 0; i < members.length; i++) {
   						if (members[i].getType() == IResource.FOLDER) folders.add(members[i]);
   					}
   					return folders.toArray();
   				} catch (CoreException e) {
   					// TODO Auto-generated catch block
   					e.printStackTrace();
   				}
   			}
   			return NO_CHILDREN;
   		}

   		public Object getParent(Object element) {
   			if (element instanceof IContainer) {
   				return ((IContainer) element).getParent();
   			}
   			return null;
   		}

   		public boolean hasChildren(Object element) {
   			if (element instanceof IContainer) {
   				IContainer container = (IContainer) element;
   				try {
   					IResource members[] = container.members();
   					List folders = new ArrayList();
   					for (int i = 0; i < members.length; i++) {
   						if (members[i].getType() == IResource.FOLDER) folders.add(members[i]);
   					}
   					return folders.size() > 0;
   				} catch (CoreException e) {
   					// TODO Auto-generated catch block
   					e.printStackTrace();
   				}
   			}
   			return false;
   		}

   		public Object[] getElements(Object inputElement) {
   			if (inputElement instanceof Collection)
   				return ((Collection)inputElement).toArray();
   			
   			return NO_CHILDREN;
   		}

   		public void dispose() {}
   		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
   	 }
   	 
   	 public static class FolderLabelProvider implements ILabelProvider {
   		public FolderLabelProvider () {}
   		public Image getImage(Object element) {
   			if (element instanceof IProject) {
   				return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
   			} else if (element instanceof IFolder) {
   				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
   			}
   			return null;
   		}

   		public String getText(Object element) {
   			if (element instanceof IContainer) {
   				return ((IContainer)element).getName();
   			}
   			return "";
   		}

   		public void addListener(ILabelProviderListener listener) {
   		}

   		public void dispose() {
   		}

   		public boolean isLabelProperty(Object element, String property) {
   			return true;
   		}

   		public void removeListener(ILabelProviderListener listener) {
   		}
   		 
   	 }
   }

}
