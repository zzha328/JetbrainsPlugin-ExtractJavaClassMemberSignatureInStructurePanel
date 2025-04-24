package com.zzha0328.ExtractMemberSignature;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;

import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

public class CopyMemberSignature extends AnAction {

    private final NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup("CopyMemberSignatureNotificationGroup");
    private final List<IElementType> stoppingJavaTokenTypes = List.of(
            JavaTokenType.LBRACE,
            JavaTokenType.EQ,
            JavaTokenType.SEMICOLON);

    @Override
    public void actionPerformed(AnActionEvent e) {
        String notificationMessage = "";
        PsiElement[] elements = e.getData(PlatformDataKeys.PSI_ELEMENT_ARRAY);  // get selected elements

        if (elements == null) {
            showNotification("No elements selected.");
            return;
        }

        List<String> extractedSignatureTexts = new ArrayList<>();
        for (PsiElement element : elements) {
            if (element instanceof PsiMember) {
                String memberSignatureText = extractMemberSignatureText(element);
                extractedSignatureTexts.add(memberSignatureText);
            }
        }

        String result = String.join("\n", extractedSignatureTexts);

        // copy to clipboard
        String failedMessage = copyToClipboard(result);
        if (failedMessage.isBlank()) {
            notificationMessage += "Success copying member signature.";
        } else {
            notificationMessage += "Failed copying member signature: " + failedMessage;
        }

        showNotification(notificationMessage);
    }

    private String extractMemberSignatureText(PsiElement element) {
        StringBuilder extractedText = new StringBuilder();
        PsiElement[] childElements = element.getChildren();

        boolean getTextFlag = false;
        for (PsiElement child : childElements) {
            // start until meeting the modifier
            if (child instanceof PsiModifierList) {
                getTextFlag = true;
                // skip annotation texts
                // TODO: so many iterations
                PsiElement[] modifierListChildren = child.getChildren();
                for (PsiElement modifier : modifierListChildren) {
                    if (!(modifier instanceof PsiAnnotation)) {
                        extractedText.append(modifier.getText());
                    }
                }
                continue;
            }

            // stop until meeting a "{...}"
            if (child instanceof PsiCodeBlock) {
                break;
            }

            /* stop until meeting a java token of LBRACE('{') or EQ('=') or SEMICOLON(';')
            * Example:
            * class: public class example { -> public class example
            * field: public String example = "example"; -> public String example
            * abstract method: void example(); -> void example()
            * */
            if (child instanceof PsiJavaToken) {
                IElementType tokenType = ((PsiJavaToken) child).getTokenType();
                if (stoppingJavaTokenTypes.contains(tokenType)) {
                    break;
                }
            }

            if (getTextFlag) {
                extractedText.append(child.getText());
            }
        }

        return extractedText.toString()
                .replaceAll("\\s+", " ") // remove line breaks
                .trim();
    }

    private void showNotification(String message) {
        Notification notification = group.createNotification(
                "Extracted signature texts",
                message,
                NotificationType.INFORMATION);

        Notifications.Bus.notify(notification);
    }

    private String copyToClipboard(String text) {
        try {
            CopyPasteManager.getInstance().setContents(new StringSelection(text));
            return "";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
