package masters.utils;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.management.RuntimeErrorException;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import masters.Constants;
import masters.grid.GridPoint;
import masters.image.ImageDTO;
import masters.image.PixelDTO;
import masters.superpixel.SuperPixelDTO;
import masters.train.WeightVector;

public class DataHelper {
	private static List<String>	SEGMENTED_HEX_COLOURS = Constants.SEGMENTED_HEX_COLOURS;
	private static Map<Integer,Color> LABEL_TO_COLOUR_MAP = Constants.LABEL_TO_COLOUR_MAP;
	
	public static String TEST_TEST_SET = Constants.TEST_PATH;
	public static String TEST_TRAIN_SET = Constants.TRAIN_PATH;
	public static String TEST_SEGMENTATION_RESULTS = Constants.RESULT_PATH;
	
	public static String IMAGE_EXTENSION = Constants.IMAGE_EXTENSION;
	public static String RESULT_IMAGE_SUFFIX = Constants.RESULT_IMAGE_SUFFIX;
	
	/* getters */ 

	
	public static List<ImageDTO> getTrainingDataTestSegmented() {
		Map<String, File> trainingFiles = getFilesFromDirectory(TEST_TRAIN_SET);
		Map<String, File> resultFiles = getFilesFromDirectory(TEST_SEGMENTATION_RESULTS);
		
		List <ImageDTO> imageList = new ArrayList<ImageDTO>();
		int i = 0;
		for (String key : trainingFiles.keySet()) {
			if (++i >= Constants.TRAIN_IMAGE_LIMIT) {
				return imageList;
			}
			
			File trainFile = trainingFiles.get(key);
			File segmentedFile = resultFiles.get(key + RESULT_IMAGE_SUFFIX);
			
			BufferedImage trainImg = openImage(trainFile.getPath());
			BufferedImage segmentedImg = openImage(segmentedFile.getPath());
			
			ImageDTO imageObj = new ImageDTO(trainFile.getPath(), trainImg.getWidth(),trainImg.getHeight(), trainImg);
			PixelDTO[][] pixelData = getPixelDTOs(trainImg, false);
			imageObj.setPixelData(pixelData);
			
			PixelDTO[][] segmentedPixelData = getPixelDTOs(segmentedImg, true);
			updateLabelFromSegmentedImage(imageObj,segmentedPixelData);
			
			imageList.add(imageObj);
		}
		return imageList;
	}

	public static List<ImageDTO> getTestData() {
		Map<String, File> testFiles = getFilesFromDirectory(TEST_TEST_SET);
		
		List <ImageDTO> imageList = new ArrayList<ImageDTO>();
		int i = 0;
		for (String key : testFiles.keySet()) {
			if (++i >= Constants.TEST_IMAGE_LIMIT) {
				break;
			}
			File trainFile = testFiles.get(key);
			
			BufferedImage trainImg = openImage(trainFile.getPath());
			
			ImageDTO imageObj = new ImageDTO(trainFile.getPath(), trainImg.getWidth(),trainImg.getHeight(), trainImg);
			PixelDTO[][] pixelData = getPixelDTOs(trainImg, false);
			imageObj.setPixelData(pixelData);
			
			imageList.add(imageObj);
		}
		return imageList;
	}
	
	public static PixelDTO[][] getPixelDTOs (BufferedImage image, boolean isSegmented) {
		Set<String> col = new HashSet<String>();
		int[] tmpArray = null;
		WritableRaster rasterImage = image.getRaster();
		int width = image.getWidth();
		int height = image.getHeight();
		PixelDTO[][] pixelArray = new PixelDTO[width][height];
		for (int y = 0; y < height; y++) {
			  for (int x = 0; x < width; x++ ) {
				  int[] pixelData = rasterImage.getPixel(x, y, tmpArray);
				  
				  int  r = pixelData[0];
				  int  g = pixelData[1];
				  int  b = pixelData[2];
				  int alpha = 0;
				  PixelDTO pixel;
				  if (isSegmented) {
					  int label = -1;
					  String hexColor = String.format("#%02x%02x%02x", r, g, b);
					  if (!col.contains(hexColor)) {
						  col.add(hexColor);
					  }
					  for (int i = 0; i < SEGMENTED_HEX_COLOURS.size(); i++) {
						  String segmentedHexColour = SEGMENTED_HEX_COLOURS.get(i);
						  if (hexColor.equals(segmentedHexColour)) {
							  label = i;
							  break;
						  }
					  }
					  if (label == -1) {
						  _log.error("getPixelDTOs: chosen label -1 for hex colour " + hexColor);
						  throw new RuntimeErrorException(null);
					  }
					  pixel = new PixelDTO(x, y, r, g, b, alpha, label);
				  } else {
					  pixel = new PixelDTO(x, y, r, g, b, alpha, null);
				  }
				  
				  pixelArray[x][y] = pixel;
			  }
		  }
		return pixelArray;
	}
	
	
	private static Map<String,File> getFilesFromDirectory(String path) {
		File folder;
		try {
			folder = new File(DataHelper.class.getClassLoader().getResource(path).toURI());
			File[] listOfFiles = folder.listFiles();
			if (listOfFiles == null) {
				_log.error("path " + folder.getAbsolutePath() + " has no files");
				throw new RuntimeErrorException(null);
			}
			Map<String,File> result = new HashMap<String, File>();
			
			for (int i = 0; i < listOfFiles.length; i++) {
				File file = listOfFiles[i];
				result.put(FilenameUtils.removeExtension(file.getName()), file);
			}
			return result;
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new RuntimeErrorException(null);
		}
		
	}
	
	private static void updateLabelFromSegmentedImage(ImageDTO imageObj, PixelDTO[][] segmentedPixelData) {
		PixelDTO[][] pixelData = imageObj.getPixelData();
		for (int i = 0; i < pixelData[0].length; i++) {
			for (int j = 0; j < pixelData.length; j++) {
				PixelDTO pixel = pixelData[j][i];
				PixelDTO segmentedPixel = segmentedPixelData[j][i];
				int label = segmentedPixel.getLabel();
				pixel.setLabel(label);
			}
		}
	}
	public static BufferedImage openImage(String path) {
		BufferedImage img;
		try {
			img = ImageIO.read(new File(path));
			return img;
		} catch(IOException e) {
			return null;
		}
	}
	
	
	
    
	/* viewers */
	
	public static void viewImageSegmented(ImageDTO image) {
		viewImageSegmented(image, "");
	}
	public static void viewImageSegmented(ImageDTO image, String title) {
		/* change image pixel data */
		BufferedImage img = image.getImage();
		
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
            	try {
            	int value = LABEL_TO_COLOUR_MAP.get(image.getPixelData()[x][y].getLabel()).getRGB();
            	img.setRGB(x, y, value);
            	} catch (NullPointerException e) {
            		_log.error("null", e);
                	System.out.println("image.pixelData[x][y].getLabel()" + image.getPixelData()[x][y].getLabel());
                	throw new RuntimeErrorException(null);
            	}
            }
        }
        ImageIcon icon = new ImageIcon(img);
        JFrame frame=new JFrame(title);
        frame.setLayout(new FlowLayout());
        frame.setSize(img.getWidth() + 10, img.getHeight() + 30);
        JLabel lbl = new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public static void viewImageSegmentedSuperPixels(ImageDTO image, List<SuperPixelDTO> superPixels, String title) {
		for (SuperPixelDTO superPixel : superPixels) {
			superPixel.updatePixelLabels();
		}
		viewImageSegmented(image, title);
	}
	public static void viewImageSegmentedSuperPixels(ImageDTO image, List<SuperPixelDTO> superPixels, List<Integer> mask) {
		for (SuperPixelDTO superPixel : superPixels) {
			superPixel.updatePixelLabels(mask);
		}
		viewImageSegmented(image);
	}
	
	
	
	public static void viewImageSuperpixelBordersOnly(ImageDTO image, List<SuperPixelDTO> superPixels) {
		viewImageSuperpixelBordersOnly(image, superPixels, "");
	}
	public static void viewImageSuperpixelBordersOnly(ImageDTO image, List<SuperPixelDTO> superPixels, String title) {
		BufferedImage img = image.getImage();
		for (SuperPixelDTO superPixel : superPixels) {
            int rgbSuperPixel = superPixel.getIdentifingColorRGB();
			List<PixelDTO> borderPixels = superPixel.getBorderPixels();
			for (PixelDTO borderPixel : borderPixels) {
				img.setRGB(borderPixel.getXIndex(), borderPixel.getYIndex(), rgbSuperPixel);
			}
		}
        ImageIcon icon = new ImageIcon(img);
        JFrame frame = new JFrame(title);
        frame.setLayout(new FlowLayout());
        frame.setSize(img.getWidth() + 10, img.getHeight() + 30);
        JLabel lbl = new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public static void viewImageSuperpixelMeanData(ImageDTO image, List<SuperPixelDTO> superPixels) {
		viewImageSuperpixelMeanData(image, superPixels, "");
	}
	public static void viewImageSuperpixelMeanData(ImageDTO image, List<SuperPixelDTO> superPixels, String title) {
		/* change image pixel data */
		BufferedImage img = image.getImage();
		for (SuperPixelDTO sp : superPixels) {
			double[] rgbArray = sp.getMeanRGB();
			int rgb = new Color((int)rgbArray[0],(int)rgbArray[1], (int)rgbArray[2]).getRGB();
			List<PixelDTO> pixels = sp.getPixels();
			for (PixelDTO p : pixels) {
				img.setRGB(p.getXIndex(), p.getYIndex(), rgb);
			}
		}
            		
        ImageIcon icon = new ImageIcon(img);
        JFrame frame = new JFrame(title);
        frame.setLayout(new FlowLayout());
        frame.setSize(img.getWidth() + 10, img.getHeight() + 30);
        JLabel lbl = new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	/* savers */
	
	public static void saveImageSuperpixelBordersOnly(ImageDTO image, List<SuperPixelDTO> superPixels, String path) {
		BufferedImage img = image.getImage();
		for (SuperPixelDTO superPixel : superPixels) {
            int rgbSuperPixel = superPixel.getIdentifingColorRGB();
			List<PixelDTO> borderPixels = superPixel.getBorderPixels();
			for (PixelDTO borderPixel : borderPixels) {
				img.setRGB(borderPixel.getXIndex(), borderPixel.getYIndex(), rgbSuperPixel);
			}
		}
		File outputFile = new File(path);
		try {
			ImageIO.write(img, Constants.IMAGE_EXTENSION, outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void saveImageSuperpixelMeanData(ImageDTO image, List<SuperPixelDTO> superPixels, String path) {
		BufferedImage img = image.getImage();
		/* change image pixel data */
		for (SuperPixelDTO sp : superPixels) {
			double[] rgbArray = sp.getMeanRGB();
			int rgb = new Color((int)rgbArray[0],(int)rgbArray[1], (int)rgbArray[2]).getRGB();
			List<PixelDTO> pixels = sp.getPixels();
			for (PixelDTO p : pixels) {
				img.setRGB(p.getXIndex(), p.getYIndex(), rgb);
			}
		}
            		
        File outputFile = new File(path);
		try {
			ImageIO.write(img, Constants.IMAGE_EXTENSION, outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void saveImageSegmentedWithMask(ImageDTO image, List<SuperPixelDTO> superPixels, List<Integer> mask, String path) {
		try {
			File outputfile = new File(path);
			outputfile.createNewFile();
			
			for (SuperPixelDTO superPixel : superPixels) {
				superPixel.updatePixelLabels(mask);
			}
			/* change image pixel data */
			BufferedImage img = image.getImage();
			
	        for (int x = 0; x < img.getWidth(); x++) {
	            for (int y = 0; y < img.getHeight(); y++) {
	            	int value = LABEL_TO_COLOUR_MAP.get(image.getPixelData()[x][y].getLabel()).getRGB();
	            	img.setRGB(x, y, value);
	            }
	        }
	        /* update borders */
	        for (SuperPixelDTO superPixel : superPixels) {
	            int rgbSuperPixel = superPixel.getIdentifingColorRGB();
				List<PixelDTO> borderPixels = superPixel.getBorderPixels();
				for (PixelDTO borderPixel : borderPixels) {
					img.setRGB(borderPixel.getXIndex(), borderPixel.getYIndex(), rgbSuperPixel);
				}
			}
	        
	        /* update superpixel index */
	        Graphics graphics = img.getGraphics();
	        graphics.setFont(new Font("Arial Black", Font.BOLD, 20));
	        for (SuperPixelDTO superPixel : superPixels) {
	        	GridPoint middle = superPixel.getSamplePixel();
	        	Color myColor =  new Color(superPixel.getIdentifingColorRGB());
	        	graphics.setColor(myColor);
	        	graphics.drawString(String.valueOf(superPixel.getSuperPixelIndex()), middle.x, middle.y);
	        }
	        
			ImageIO.write(img, Constants.IMAGE_EXTENSION, outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
	}
	
	public static void saveImage(ImageDTO imageObj, String path){
		try {
			File outputfile = new File(path);
			outputfile.createNewFile();
			/* change image pixel data */
			BufferedImage originalImage = imageObj.getImage();
			
			int width = originalImage.getWidth();
	        int height = originalImage.getHeight();
	        int[][] pixel = new int[width][height];
	        Raster raster = originalImage.getData();
	        for (int i = 0; i < width; i++) {
	            for (int j = 0; j < height; j++) {
	                pixel[i][j] = raster.getSample(i, j, 0);
	            }
	        }
	        BufferedImage theImage = new BufferedImage(width, height,
	                BufferedImage.TYPE_INT_RGB);
	        for (int i = 0; i < width; i++) {
	            for (int j = 0; j < height; j++) {
	            	PixelDTO pixelDTO = imageObj.getPixelData()[i][j];
	            	int value;
	            	value = LABEL_TO_COLOUR_MAP.get(pixelDTO.getLabel()).getRGB();
	                theImage.setRGB(i, j, value);
	            }
	        }
	        ImageIO.write(theImage, Constants.IMAGE_EXTENSION, outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* others */
	
	
	public static Set <String> getPixelColors(BufferedImage image){
	  Set <String> hashColours = new HashSet<String>();
	  for (int x = 0; x < image.getWidth(); x++) {
		  for (int y = 0; y < image.getHeight(); y++ ) {
			  int clr=  image.getRGB(x,y); 
			  int  r   = (clr & 0x00ff0000) >> 16;
			  int  g = (clr & 0x0000ff00) >> 8;
			  int  b  =  clr & 0x000000ff;
			  
			  String hex = String.format("#%02x%02x%02x", r, g, b);  
			  hashColours.add(hex);
		  }
	  }
	  return hashColours;
  }
  
	public static void saveWeights(WeightVector weightVector) {
		String pathName = "resources/weights.txt";
		File outputFile = new File(pathName);
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (double weight : weightVector.getWeights()) {
			sb.append(weight);
			sb.append(", ");
		}
		sb.append(")");
		try {
			FileUtils.writeStringToFile(outputFile, sb.toString(), Charset.defaultCharset(), false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
  
  private static Logger _log = Logger.getLogger(DataHelper.class);


}