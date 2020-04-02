package jp.kitabatakep.intellij.plugins.codereadingrecorder.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.util.messages.MessageBus;
import jp.kitabatakep.intellij.plugins.codereadingrecorder.AppConstants;
import jp.kitabatakep.intellij.plugins.codereadingrecorder.CodeReadingRecorderService;
import jp.kitabatakep.intellij.plugins.codereadingrecorder.TopicListImporter;
import jp.kitabatakep.intellij.plugins.codereadingrecorder.TopicListNotifier;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ImportAction extends AnAction
{
    public ImportAction() {
        super("Import", "Import", AllIcons.ToolbarDecorator.Import);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e)
    {
        Project project = e.getProject();
        CodeReadingRecorderService service = CodeReadingRecorderService.getInstance(project);

        VirtualFile baseDir;
        if (!service.lastImportDir().equals("")) {
            baseDir = LocalFileSystem.getInstance().findFileByPath(service.lastImportDir());
        } else {
            baseDir = LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home"));
        }

        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("xml");
        VirtualFile[] files = FileChooserFactory.getInstance().
            createFileChooser(fileChooserDescriptor, project, null).
            choose(project, baseDir);

        if (files.length == 0) {
            return;
        }

        VirtualFile parentDir = files[0].getParent();
        if (parentDir != null && parentDir.exists()) {
            service.setLastImportDir(parentDir.getPath());
        }

        SAXBuilder builder = new SAXBuilder();
        Document document = null;
        try {
             document = builder.build(new File(files[0].getPath()));
        } catch (JDOMException ex) {
            Messages.showErrorDialog(project, "Fail to load action caused by illegal format file content.", AppConstants.appName + "Load");
        } catch (IOException ex) {
            Messages.showErrorDialog(project, "Fail to load action. Please try again.", AppConstants.appName + "Load");
            return;
        }

        if (document == null) {
            return;
        }

        try {
            service.getTopicList().setTopics(TopicListImporter.importElement(project, document.getRootElement()));
            MessageBus messageBus = project.getMessageBus();
            TopicListNotifier publisher = messageBus.syncPublisher(TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC);
            publisher.topicsLoaded();
        } catch (TopicListImporter.FormatException _) {
            Messages.showErrorDialog(
                project,
                "Fail to load action caused by illegal format file content.",
                AppConstants.appName + "Load"
            );
        }


    }
}
