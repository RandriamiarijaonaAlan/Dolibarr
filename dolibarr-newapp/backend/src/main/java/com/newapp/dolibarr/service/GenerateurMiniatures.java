package com.newapp.dolibarr.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

/**
 * Génère les miniatures des photos utilisateurs côté backend, en Java pur (ImageIO/Graphics2D),
 * sans dépendance externe. Remplace l'usage de sharp/jimp (qui supposait un backend Node).
 *
 * ATTENTION : les dimensions Dolibarr sont exprimées en (hauteur, largeur). Les méthodes
 * ci-dessous prennent donc explicitement largeur ET hauteur pour éviter toute inversion.
 */
@Service
public class GenerateurMiniatures {

    /**
     * Redimensionne une image à la taille exacte demandée et renvoie son contenu encodé.
     *
     * @param source  contenu binaire de l'image d'origine
     * @param largeur largeur cible en pixels
     * @param hauteur hauteur cible en pixels
     * @param format  "png" ou "jpg" (format de sortie pour ImageIO)
     * @return contenu binaire de la miniature
     */
    public byte[] redimensionner(byte[] source, int largeur, int hauteur, String format) throws IOException {
        BufferedImage origine = ImageIO.read(new ByteArrayInputStream(source));
        if (origine == null) {
            throw new IOException("Image illisible ou format non supporté");
        }

        // Le JPEG ne gère pas la transparence : on force un type sans canal alpha pour ce format.
        int type = "jpg".equalsIgnoreCase(format) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage miniature = new BufferedImage(largeur, hauteur, type);

        Graphics2D graphique = miniature.createGraphics();
        graphique.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphique.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphique.drawImage(origine, 0, 0, largeur, hauteur, null);
        graphique.dispose();

        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        if (!ImageIO.write(miniature, format, sortie)) {
            throw new IOException("Aucun encodeur ImageIO disponible pour le format " + format);
        }
        return sortie.toByteArray();
    }
}
