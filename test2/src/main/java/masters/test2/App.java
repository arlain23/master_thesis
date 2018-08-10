package masters.test2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import masters.test2.colors.ColorSpaceException;
import masters.test2.factorisation.FactorGraphModel;
import masters.test2.image.ImageDTO;
import masters.test2.superpixel.SuperPixelDTO;
import masters.test2.superpixel.SuperPixelHelper;
import masters.test2.train.GradientDescentTrainer;
import masters.test2.train.WeightVector;
import masters.test2.utils.DataHelper;
import masters.test2.utils.ProbabilityContainer;

/**
 * Hello world!
 *
 */
public class App 
{
	public static boolean PRINT = false;
    public static void main( String[] args ) throws ColorSpaceException
    {
        System.out.println(  System.getProperty("user.dir") );
		List<ImageDTO> imageList = DataHelper.getTrainingDataTestSegmented();
		
		
		Map<ImageDTO, List<SuperPixelDTO>> superPixelMap = new HashMap<ImageDTO, List<SuperPixelDTO>>();
		Map<ImageDTO, FactorGraphModel> imageToFactorGraphMap = new HashMap<ImageDTO, FactorGraphModel>();
		
		WeightVector randomWeightVector = new WeightVector(Constants.NUMBER_OF_STATES, SuperPixelDTO.NUMBER_OF_LOCAL_FEATURES, SuperPixelDTO.NUMBER_OF_PAIRWISE_FEATURES);
		int iterator = 0;
		// factorisation
		for (ImageDTO currentImage : imageList) {
			//dh.viewImage(currentImage);
			//DataHelper.viewImageSegmented(currentImage);
			List<SuperPixelDTO> createdSuperPixels = SuperPixelHelper.getSuperPixel(currentImage, Constants.NUMBER_OF_SUPERPIXELS, Constants.RIGIDNESS);
			SuperPixelHelper.updateSuperPixelLabels(createdSuperPixels);
			superPixelMap.put(currentImage, createdSuperPixels);
			FactorGraphModel factorGraph = new FactorGraphModel(currentImage,createdSuperPixels, randomWeightVector, null);
			imageToFactorGraphMap.put(currentImage, factorGraph);
		//	DataHelper.increaseBlue(createdSuperPixels);
			
			DataHelper.viewImageSuperpixelBordersOnly(currentImage, createdSuperPixels, "train " + (iterator) + " borders");
			DataHelper.viewImageSuperpixelMeanData(currentImage, createdSuperPixels, "train " + (iterator) + " mean");
			DataHelper.viewImageSegmentedSuperPixels(currentImage, createdSuperPixels, "train " + (iterator) + " segmented");
//			DataHelper.saveImageSuperpixelBordersOnly(currentImage, createdSuperPixels, "C:\\Users\\anean\\Desktop\\hoho_a_" + iterator + ".png");
			iterator ++;
		}
		
		//Data holders
		ProbabilityContainer probabiltyContainer = new ProbabilityContainer(imageToFactorGraphMap);
		
		// training
		List<Double> initWeightList = Arrays.asList(new Double[] {
				0.033156268062451706, 0.03281594178569262, 0.050641381268472764, 0.050945967364127494, 0.0016190301973335545 
 

 
		});
		

		WeightVector pretrainedWeights = new WeightVector(initWeightList);

		
		GradientDescentTrainer trainer = new GradientDescentTrainer(imageList, imageToFactorGraphMap, probabiltyContainer);
//		WeightVector weights = pretrainedWeights;
		WeightVector weights = trainer.train(null);
		System.out.println("final weights");
		System.out.println(weights);
		
		
		// get test image
		List<ImageDTO> testImageList = DataHelper.getTestData();
		System.out.println("");
		System.out.println("Performing testing");
		int imageCounter = 0;
		for (ImageDTO currentImage : testImageList) {
			List<SuperPixelDTO> createdSuperPixels = SuperPixelHelper.getSuperPixel(currentImage, Constants.NUMBER_OF_SUPERPIXELS, Constants.RIGIDNESS);
			FactorGraphModel factorGraph = new FactorGraphModel(currentImage,createdSuperPixels, weights, imageToFactorGraphMap, probabiltyContainer);
			
			DataHelper.viewImageSuperpixelBordersOnly(currentImage, createdSuperPixels, ("test " + imageCounter));
			DataHelper.viewImageSuperpixelMeanData(currentImage, createdSuperPixels, "test " + (imageCounter) + " mean");
//			DataHelper.saveImageSuperpixelBordersOnly(currentImage, createdSuperPixels, "C:\\Users\\anean\\Desktop\\hoho_a_" + imageCounter + ".png");
//			DataHelper.saveImageSuperpixelMeanData(currentImage, createdSuperPixels, "C:\\Users\\anean\\Desktop\\hoho_b_" + imageCounter + ".png");
			
			
			// inference 
			for (int t = 0; t < 20; t++) {
				factorGraph.computeFactorToVariableMessages();
				factorGraph.computeVariableToFactorMessages();

				factorGraph.updatePixelData();
				factorGraph.computeLabeling();
				
				boolean shown = true;
				if (t%5 == 0) {
					shown = true;
					DataHelper.viewImageSegmentedSuperPixels(factorGraph.getImage(), createdSuperPixels, "test " + imageCounter + " inference "+ t);
					
				}
				if (factorGraph.checkIfConverged()) {
					System.out.println("converged");
					factorGraph.updatePixelData();
					if (!shown) {
						DataHelper.viewImageSegmentedSuperPixels(factorGraph.getImage(), createdSuperPixels, "test " + imageCounter + " inference " + t);
					}
					//break;
				}
			}
			factorGraph.computeLabeling();
			DataHelper.viewImageSegmentedSuperPixels(factorGraph.getImage(), createdSuperPixels, "test " + imageCounter + " final result");
			String filePath = "E:\\Studia\\CSIT\\praca_magisterska\\output" + (++imageCounter) + ".png";
			//DataHelper.saveImage(factorGraph.getImage(), filePath);
			System.out.println("finished for image " + imageCounter);
			System.out.println();
		}
    }
}
