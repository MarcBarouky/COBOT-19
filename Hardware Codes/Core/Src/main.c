/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.c
  * @brief          : Main program body
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2022 STMicroelectronics.
  * All rights reserved.
  *
  * This software is licensed under terms that can be found in the LICENSE file
  * in the root directory of this software component.
  * If no LICENSE file comes with this software, it is provided AS-IS.
  *
  ******************************************************************************
  */
/* USER CODE END Header */
/* Includes ------------------------------------------------------------------*/
#include "main.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include "stdio.h"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */
/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */
int len = 0;
char m[100];


/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
TIM_HandleTypeDef htim1;
TIM_HandleTypeDef htim2;
TIM_HandleTypeDef htim3;
TIM_HandleTypeDef htim4;

UART_HandleTypeDef huart1;
UART_HandleTypeDef huart2;
UART_HandleTypeDef huart3;

/* USER CODE BEGIN PV */

/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
static void MX_GPIO_Init(void);
static void MX_TIM1_Init(void);
static void MX_USART1_UART_Init(void);
static void MX_USART2_UART_Init(void);
static void MX_TIM3_Init(void);
static void MX_TIM4_Init(void);
static void MX_USART3_UART_Init(void);
static void MX_TIM2_Init(void);
/* USER CODE BEGIN PFP */

/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */

char wref1 = 64;
char wref2 = 64;

char ID1=1;
char ID2 = 99;

int cntW = 0;
char w[1];


// for tablet

//char t[1];

char t[1];
int cntT = 0;




int Speed = 0;
int LEDMODE = 0;

int LEDSTEP = 1000;
int LEDR= 0;
int LEDG = 0;
int LEDB = 0;
int VRmax = 50;
int buzzer = 0;
int pump=0;
int arm=0;
int cnt=0;
int obstaclemode=0;
char direction[2];


void LEDFCN()
{
	if(LEDMODE == 0)
	{
		htim4.Instance->CCR1 = 0;	// Red
		htim4.Instance->CCR2 = 0;
		htim4.Instance->CCR3 = 0;
	}
	else if(LEDMODE == 1)
	{
		htim4.Instance->CCR1 = 7100;	//Blue
		htim4.Instance->CCR2 = 0;
		htim4.Instance->CCR3 = 0;

	}
	else if(LEDMODE == 2)				//Green
	{
		htim4.Instance->CCR1 = 0;
		htim4.Instance->CCR2 = 7100;
		htim4.Instance->CCR3 = 0;
	}
	else if(LEDMODE == 3)			//Multicolor
	{
		htim4.Instance->CCR1 = 0;
		htim4.Instance->CCR2 = 0;
		htim4.Instance->CCR3 = 7100;
	}
	else if(LEDMODE == 4)
	{
		LEDR = LEDR+ LEDSTEP;
		if(LEDR >=7200)
		{
			LEDR = 0;
			LEDG = LEDG + LEDSTEP;
			if(LEDG >=7200)
			{
				LEDG = 0;
				LEDB = LEDB + LEDSTEP;
				if(LEDB >= 7200)
				{
					LEDB = 0;
				}
			}
		}		
		htim4.Instance->CCR1 = LEDR;
		htim4.Instance->CCR2 = LEDG;
		htim4.Instance->CCR3 = LEDB;
	}
}


void PumpFCN(){

	if( pump == 1)									//function that toggles the state of the pump through a digital pin
	{
		HAL_GPIO_TogglePin(GPIOB, GPIO_PIN_12);

		pump=0;

	}
}
		
void ReceiveWref()									//function that receives the commands from the MIT app
{
	HAL_UART_Receive(&huart1,w,1,0x0005);

	if(cntW==0)
	{	
		if(w[0]=='F')
			cntW=100;
		else if(w[0]=='B')		// depending on the first letter received cntW takes a value
			cntW = 110;
		else if(w[0]=='R')
			cntW = 120;
		else if(w[0]=='S')
			cntW = 130;
		else if(w[0]=='L')
			cntW = 140;
		else if(w[0]=='C')
			cntW = 150;
		else if(w[0]=='V')
			cntW = 160;
		else if(w[0]=='Y')
			cntW=170;
		else if(w[0]=='N')
			cntW=180;
		else
			cntW = 0;
	}
	else if(cntW == 100) 	//cntW=100 meaning the first letter received is F
	{
		if(w[0]=='F') 	//second letter is F so FF is received
		{
			wref1 = VRmax + 64;					//wref1 & wref2 are the speeds of the motors
			wref2 = -VRmax  + 64;
			htim3.Instance->CNT=1;
			cntW = 0;
			direction[0]='F';				//in all this function direction is a variable to stock the direction of the cobot's movement
			direction[1]='F';				// the purpose of stocking this direction is to use it in the External interrupt of the digital pins code
		}									// where for every direction the cobot is moving specific sensors are tested
		else if(w[0]=='R')
		{
			wref1 = VRmax + 64;
			wref2 = -5  + 64;
			htim3.Instance->CNT=1;
			cntW = 0;
			direction[0]='F';
			direction[1]='R';
		}
		else if(w[0]=='L')
		{
			wref1 = 5 + 64;
			wref2 = -VRmax  + 64;
			htim3.Instance->CNT=1;
			cntW = 0;
			direction[0]='F';
			direction[1]='L';
		}
		else
			cntW = 0;
	}
	else if(cntW == 110)
	{
		if(w[0]=='B')
		{
			wref1 = -VRmax + 64;
			wref2 = VRmax  + 64;
			htim3.Instance->CNT=1;
			cntW = 0;
			direction[0]='B';
			direction[1]='B';
		}
		else if(w[0]=='R')
		{
			wref1 = -VRmax + 64;
			wref2 = 5  + 64;
			htim3.Instance->CNT=1;
			cntW = 0;
			direction[0]='B';
			direction[1]='R';
		}
		else if(w[0]=='L')
		{
			wref1 = -5 + 64;
			wref2 = VRmax  + 64;
			htim3.Instance->CNT=1;
			cntW = 0;
			direction[0]='B';
			direction[1]='L';
		}
		else
			cntW = 0;
	}
	else if(cntW == 120)
	{
		if(w[0]=='R')
		{
			wref1 = VRmax + 64;
			wref2 = VRmax  + 64;
			htim3.Instance->CNT=1;
			cntW = 0;
			direction[0]='R';
			direction[1]='R';
		}
		else
			cntW = 0;
	}
	else if(cntW == 130)
	{
		if(w[0]=='S')
		{
			wref1 = 0 + 64;
			wref2 = 0  + 64;
			htim3.Instance->CNT=1;
			cntW = 0;
		}
		else
			cntW = 0;
	}
	else if(cntW == 140)
	{
		if(w[0]=='L')
		{
			wref1 = -VRmax + 64;
			wref2 = -VRmax  + 64;
			htim3.Instance->CNT=1;
			cntW = 0;
			direction[0]='L';
			direction[1]='L';
		}
		else
			cntW = 0;
	}
	else if(cntW == 150)
	{
		if(w[0]=='O')
		{
			LEDMODE = 0;
			cntW = 0;
		}
		else if(w[0]=='R')
		{
			LEDMODE = 1;
			arm=1;

			cntW = 0;
		}
		else if(w[0]=='G')
		{
			LEDMODE = 2;
			cntW = 0;
		}
		else if(w[0]=='B')
		{
			LEDMODE = 3;
			cntW = 0;
		}
		else if(w[0]=='A')
		{
			LEDMODE = 4;
			cntW = 0;
		}
		else if(w[0]=='Z')
		{
			pump = 1;
			cntW = 0;
		}
		else
			cntW = 0;
	}
	else if(cntW == 160)
	{
		VRmax = (unsigned int)(w[0])*64.f/100.f; // info slider(app) maximum value = 100 
		if(VRmax>64)
			VRmax = 64;
		else if(VRmax<0)
			VRmax = 0;
		
		cntW = 0;
	}

	else if(cntW == 170)
	{
		if(w[0]=='E')
		{
			obstaclemode=1;
			HAL_GPIO_WritePin(GPIOB, GPIO_PIN_13, 1);

		}
		else
			cntW = 0;
	}

	else if(cntW == 180)
	{
		if(w[0]=='O')
		{
			obstaclemode=0;

			HAL_GPIO_WritePin(GPIOB, GPIO_PIN_13, 0);
		}
		else
			cntW = 0;
	}



	else
		cntW=0;
	
}

void TransmitWref()								//function that transfers the wref1 & wref2 via uart 2 to the STMs
{																// that control the motors
		len = sprintf(m,"sss");
		HAL_UART_Transmit(&huart2,m,len,0xFFFF);
		HAL_UART_Transmit(&huart2,&ID1,1,0xFFFF);
		HAL_UART_Transmit(&huart2,&wref1,1,0xFFFF);
		HAL_UART_Transmit(&huart2,&wref2,1,0xFFFF);
		len = sprintf(m,"eee");
		HAL_UART_Transmit(&huart2,m,len,0xFFFF);
		
		
		len = sprintf(m,"sss");
		HAL_UART_Transmit(&huart2,m,len,0xFFFF);
		HAL_UART_Transmit(&huart2,&ID2,1,0xFFFF);
		HAL_UART_Transmit(&huart2,&wref1,1,0xFFFF);
		HAL_UART_Transmit(&huart2,&wref2,1,0xFFFF);
		len = sprintf(m,"eee");
		HAL_UART_Transmit(&huart2,m,len,0xFFFF);
	
	
		len = sprintf(m,"sss");
		HAL_UART_Transmit(&huart1,m,len,0xFFFF);
		HAL_UART_Transmit(&huart1,&ID1,1,0xFFFF);
		HAL_UART_Transmit(&huart1,&wref1,1,0xFFFF);
		HAL_UART_Transmit(&huart1,&wref2,1,0xFFFF);
		len = sprintf(m,"eee");
		HAL_UART_Transmit(&huart1,m,len,0xFFFF);
		
		
		len = sprintf(m,"sss");
		HAL_UART_Transmit(&huart1,m,len,0xFFFF);
		HAL_UART_Transmit(&huart1,&ID2,1,0xFFFF);
		HAL_UART_Transmit(&huart1,&wref1,1,0xFFFF);
		HAL_UART_Transmit(&huart1,&wref2,1,0xFFFF);
		len = sprintf(m,"eee");
		HAL_UART_Transmit(&huart1,m,len,0xFFFF);
}



void ReceiveTablet()						// function that receives the orders from the Tablet
{
	HAL_UART_Receive(&huart3,t,1,0x0005);

	if(cntT==0)
	{
		if(t[0]=='H')
			cntT=100;
		else
			cntT = 0;
	}
	else if(cntT == 100)
	{
		arm=1;
		LEDMODE = 3;
		cntT=0;
	}

	else
		cntT=0;

}

void DelayTwoSeconds(){						//Delay that was created using a timer to avoid HAL_Delays
											//that might cause problems in the interruptions
	cnt=0;

	while ( cnt<100 ){


	}


}

void ActivateArm(){							//Function that Activates the movement of the arm by sending a PWM signal to the Servo

	if (arm==1){

	htim2.Instance->CCR2=125;				// Servo to position 180 degrees
	DelayTwoSeconds();
	htim2.Instance->CCR2=25;				//Servo to position 0	degrees
	DelayTwoSeconds();

	arm=0;

	}



}



/* USER CODE END 0 */

/**
  * @brief  The application entry point.
  * @retval int
  */
int main(void)
{
  /* USER CODE BEGIN 1 */

  /* USER CODE END 1 */

  /* MCU Configuration--------------------------------------------------------*/

  /* Reset of all peripherals, Initializes the Flash interface and the Systick. */
  HAL_Init();

  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

  /* Configure the system clock */
  SystemClock_Config();

  /* USER CODE BEGIN SysInit */

  /* USER CODE END SysInit */

  /* Initialize all configured peripherals */
  MX_GPIO_Init();
  MX_TIM1_Init();
  MX_USART1_UART_Init();
  MX_USART2_UART_Init();
  MX_TIM3_Init();
  MX_TIM4_Init();
  MX_USART3_UART_Init();
  MX_TIM2_Init();
  /* USER CODE BEGIN 2 */

	// UART
__HAL_UART_ENABLE(&huart1);
__HAL_UART_ENABLE_IT(&huart1,UART_IT_RXNE);

__HAL_UART_ENABLE(&huart3);
__HAL_UART_ENABLE_IT(&huart3,UART_IT_RXNE);


//Timer 2

__HAL_TIM_ENABLE(&htim2);
__HAL_TIM_ENABLE_IT(&htim2,TIM_IT_UPDATE);
HAL_TIM_PWM_Start(&htim2, TIM_CHANNEL_2);



// Timer 1  
// for inner loop and pwm
	htim1.Instance->ARR = 7200;
	htim1.Instance->PSC = 5000;
	htim1.Instance->CNT=0;
__HAL_TIM_ENABLE(&htim1);
__HAL_TIM_ENABLE_IT(&htim1,TIM_IT_UPDATE);

// Timer 4
	htim4.Instance->ARR = 7200;
	htim4.Instance->PSC = 1;
	htim4.Instance->CNT=0;
__HAL_TIM_ENABLE(&htim4);
__HAL_TIM_ENABLE_IT(&htim4,TIM_IT_UPDATE);

	htim4.Instance->CCER |= (uint32_t)(TIM_CCx_ENABLE << TIM_CHANNEL_1);
	htim4.Instance->CCER |= (uint32_t)(TIM_CCx_ENABLE << TIM_CHANNEL_2);
	htim4.Instance->CCER |= (uint32_t)(TIM_CCx_ENABLE << TIM_CHANNEL_3);
	htim4.Instance->CCER |= (uint32_t)(TIM_CCx_ENABLE << TIM_CHANNEL_4);
	
	htim4.Instance->CCR1 = 0;
	htim4.Instance->CCR2 = 0;
	htim4.Instance->CCR3 = 0;
	htim4.Instance->CCR4 = 0;

// Timer 3 
	htim3.Instance->ARR = 7200;
	htim3.Instance->PSC = 50000;
	htim3.Instance->CNT=0;
__HAL_TIM_ENABLE(&htim3);
__HAL_TIM_ENABLE_IT(&htim3,TIM_IT_UPDATE);

  /* USER CODE END 2 */

  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  while (1)
  {		
		if(__HAL_UART_GET_FLAG(&huart1,UART_FLAG_ORE )==1  || __HAL_UART_GET_FLAG(&huart1,UART_FLAG_NE )==1  ||__HAL_UART_GET_FLAG(&huart1,UART_FLAG_FE )==1  ||__HAL_UART_GET_FLAG(&huart1,UART_FLAG_PE )==1)
			{
				__HAL_UART_CLEAR_FLAG(&huart1, UART_FLAG_ORE);
				__HAL_UART_CLEAR_FLAG(&huart1, UART_FLAG_NE);
				__HAL_UART_CLEAR_FLAG(&huart1, UART_FLAG_FE);
				__HAL_UART_CLEAR_FLAG(&huart1, UART_FLAG_PE);
				
				MX_USART1_UART_Init();
						// UART
				__HAL_UART_ENABLE(&huart1);
				__HAL_UART_ENABLE_IT(&huart1,UART_IT_RXNE);
			}
		if(__HAL_UART_GET_FLAG(&huart3,UART_FLAG_ORE )==1  || __HAL_UART_GET_FLAG(&huart3,UART_FLAG_NE )==1  ||__HAL_UART_GET_FLAG(&huart3,UART_FLAG_FE )==1  ||__HAL_UART_GET_FLAG(&huart3,UART_FLAG_PE )==1)
					{
						__HAL_UART_CLEAR_FLAG(&huart3, UART_FLAG_ORE);
						__HAL_UART_CLEAR_FLAG(&huart3, UART_FLAG_NE);
						__HAL_UART_CLEAR_FLAG(&huart3, UART_FLAG_FE);
						__HAL_UART_CLEAR_FLAG(&huart3, UART_FLAG_PE);

						MX_USART3_UART_Init();
								// UART
						__HAL_UART_ENABLE(&huart3);
						__HAL_UART_ENABLE_IT(&huart3,UART_IT_RXNE);
					}
			
		HAL_Delay(500);
    /* USER CODE END WHILE */

    /* USER CODE BEGIN 3 */
  }
  /* USER CODE END 3 */
}

/**
  * @brief System Clock Configuration
  * @retval None
  */
void SystemClock_Config(void)
{
  RCC_OscInitTypeDef RCC_OscInitStruct = {0};
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};

  /** Initializes the RCC Oscillators according to the specified parameters
  * in the RCC_OscInitTypeDef structure.
  */
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSE;
  RCC_OscInitStruct.HSEState = RCC_HSE_ON;
  RCC_OscInitStruct.HSEPredivValue = RCC_HSE_PREDIV_DIV1;
  RCC_OscInitStruct.HSIState = RCC_HSI_ON;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_ON;
  RCC_OscInitStruct.PLL.PLLSource = RCC_PLLSOURCE_HSE;
  RCC_OscInitStruct.PLL.PLLMUL = RCC_PLL_MUL9;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
  {
    Error_Handler();
  }

  /** Initializes the CPU, AHB and APB buses clocks
  */
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_PLLCLK;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV2;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_2) != HAL_OK)
  {
    Error_Handler();
  }
}

/**
  * @brief TIM1 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM1_Init(void)
{

  /* USER CODE BEGIN TIM1_Init 0 */

  /* USER CODE END TIM1_Init 0 */

  TIM_ClockConfigTypeDef sClockSourceConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};

  /* USER CODE BEGIN TIM1_Init 1 */

  /* USER CODE END TIM1_Init 1 */
  htim1.Instance = TIM1;
  htim1.Init.Prescaler = 0;
  htim1.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim1.Init.Period = 65535;
  htim1.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim1.Init.RepetitionCounter = 0;
  htim1.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_Base_Init(&htim1) != HAL_OK)
  {
    Error_Handler();
  }
  sClockSourceConfig.ClockSource = TIM_CLOCKSOURCE_INTERNAL;
  if (HAL_TIM_ConfigClockSource(&htim1, &sClockSourceConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim1, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM1_Init 2 */

  /* USER CODE END TIM1_Init 2 */

}

/**
  * @brief TIM2 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM2_Init(void)
{

  /* USER CODE BEGIN TIM2_Init 0 */

  /* USER CODE END TIM2_Init 0 */

  TIM_ClockConfigTypeDef sClockSourceConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};
  TIM_OC_InitTypeDef sConfigOC = {0};

  /* USER CODE BEGIN TIM2_Init 1 */

  /* USER CODE END TIM2_Init 1 */
  htim2.Instance = TIM2;
  htim2.Init.Prescaler = 1440-1;
  htim2.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim2.Init.Period = 1000;
  htim2.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim2.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_Base_Init(&htim2) != HAL_OK)
  {
    Error_Handler();
  }
  sClockSourceConfig.ClockSource = TIM_CLOCKSOURCE_INTERNAL;
  if (HAL_TIM_ConfigClockSource(&htim2, &sClockSourceConfig) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_Init(&htim2) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim2, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sConfigOC.OCMode = TIM_OCMODE_PWM1;
  sConfigOC.Pulse = 0;
  sConfigOC.OCPolarity = TIM_OCPOLARITY_HIGH;
  sConfigOC.OCFastMode = TIM_OCFAST_DISABLE;
  if (HAL_TIM_PWM_ConfigChannel(&htim2, &sConfigOC, TIM_CHANNEL_1) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_ConfigChannel(&htim2, &sConfigOC, TIM_CHANNEL_2) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM2_Init 2 */

  /* USER CODE END TIM2_Init 2 */
  HAL_TIM_MspPostInit(&htim2);

}

/**
  * @brief TIM3 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM3_Init(void)
{

  /* USER CODE BEGIN TIM3_Init 0 */

  /* USER CODE END TIM3_Init 0 */

  TIM_ClockConfigTypeDef sClockSourceConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};

  /* USER CODE BEGIN TIM3_Init 1 */

  /* USER CODE END TIM3_Init 1 */
  htim3.Instance = TIM3;
  htim3.Init.Prescaler = 0;
  htim3.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim3.Init.Period = 65535;
  htim3.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim3.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_Base_Init(&htim3) != HAL_OK)
  {
    Error_Handler();
  }
  sClockSourceConfig.ClockSource = TIM_CLOCKSOURCE_INTERNAL;
  if (HAL_TIM_ConfigClockSource(&htim3, &sClockSourceConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim3, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM3_Init 2 */

  /* USER CODE END TIM3_Init 2 */

}

/**
  * @brief TIM4 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM4_Init(void)
{

  /* USER CODE BEGIN TIM4_Init 0 */

  /* USER CODE END TIM4_Init 0 */

  TIM_ClockConfigTypeDef sClockSourceConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};
  TIM_OC_InitTypeDef sConfigOC = {0};

  /* USER CODE BEGIN TIM4_Init 1 */

  /* USER CODE END TIM4_Init 1 */
  htim4.Instance = TIM4;
  htim4.Init.Prescaler = 0;
  htim4.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim4.Init.Period = 65535;
  htim4.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim4.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_Base_Init(&htim4) != HAL_OK)
  {
    Error_Handler();
  }
  sClockSourceConfig.ClockSource = TIM_CLOCKSOURCE_INTERNAL;
  if (HAL_TIM_ConfigClockSource(&htim4, &sClockSourceConfig) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_Init(&htim4) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim4, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sConfigOC.OCMode = TIM_OCMODE_PWM1;
  sConfigOC.Pulse = 0;
  sConfigOC.OCPolarity = TIM_OCPOLARITY_HIGH;
  sConfigOC.OCFastMode = TIM_OCFAST_DISABLE;
  if (HAL_TIM_PWM_ConfigChannel(&htim4, &sConfigOC, TIM_CHANNEL_1) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_ConfigChannel(&htim4, &sConfigOC, TIM_CHANNEL_2) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_ConfigChannel(&htim4, &sConfigOC, TIM_CHANNEL_3) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM4_Init 2 */

  /* USER CODE END TIM4_Init 2 */
  HAL_TIM_MspPostInit(&htim4);

}

/**
  * @brief USART1 Initialization Function
  * @param None
  * @retval None
  */
static void MX_USART1_UART_Init(void)
{

  /* USER CODE BEGIN USART1_Init 0 */

  /* USER CODE END USART1_Init 0 */

  /* USER CODE BEGIN USART1_Init 1 */

  /* USER CODE END USART1_Init 1 */
  huart1.Instance = USART1;
  huart1.Init.BaudRate = 9600;
  huart1.Init.WordLength = UART_WORDLENGTH_8B;
  huart1.Init.StopBits = UART_STOPBITS_1;
  huart1.Init.Parity = UART_PARITY_NONE;
  huart1.Init.Mode = UART_MODE_TX_RX;
  huart1.Init.HwFlowCtl = UART_HWCONTROL_NONE;
  huart1.Init.OverSampling = UART_OVERSAMPLING_16;
  if (HAL_UART_Init(&huart1) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN USART1_Init 2 */

  /* USER CODE END USART1_Init 2 */

}

/**
  * @brief USART2 Initialization Function
  * @param None
  * @retval None
  */
static void MX_USART2_UART_Init(void)
{

  /* USER CODE BEGIN USART2_Init 0 */

  /* USER CODE END USART2_Init 0 */

  /* USER CODE BEGIN USART2_Init 1 */

  /* USER CODE END USART2_Init 1 */
  huart2.Instance = USART2;
  huart2.Init.BaudRate = 115200;
  huart2.Init.WordLength = UART_WORDLENGTH_8B;
  huart2.Init.StopBits = UART_STOPBITS_1;
  huart2.Init.Parity = UART_PARITY_NONE;
  huart2.Init.Mode = UART_MODE_TX_RX;
  huart2.Init.HwFlowCtl = UART_HWCONTROL_NONE;
  huart2.Init.OverSampling = UART_OVERSAMPLING_16;
  if (HAL_UART_Init(&huart2) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN USART2_Init 2 */

  /* USER CODE END USART2_Init 2 */

}

/**
  * @brief USART3 Initialization Function
  * @param None
  * @retval None
  */
static void MX_USART3_UART_Init(void)
{

  /* USER CODE BEGIN USART3_Init 0 */

  /* USER CODE END USART3_Init 0 */

  /* USER CODE BEGIN USART3_Init 1 */

  /* USER CODE END USART3_Init 1 */
  huart3.Instance = USART3;
  huart3.Init.BaudRate = 9600;
  huart3.Init.WordLength = UART_WORDLENGTH_8B;
  huart3.Init.StopBits = UART_STOPBITS_1;
  huart3.Init.Parity = UART_PARITY_NONE;
  huart3.Init.Mode = UART_MODE_TX_RX;
  huart3.Init.HwFlowCtl = UART_HWCONTROL_NONE;
  huart3.Init.OverSampling = UART_OVERSAMPLING_16;
  if (HAL_UART_Init(&huart3) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN USART3_Init 2 */

  /* USER CODE END USART3_Init 2 */

}

/**
  * @brief GPIO Initialization Function
  * @param None
  * @retval None
  */
static void MX_GPIO_Init(void)
{
  GPIO_InitTypeDef GPIO_InitStruct = {0};
/* USER CODE BEGIN MX_GPIO_Init_1 */
/* USER CODE END MX_GPIO_Init_1 */

  /* GPIO Ports Clock Enable */
  __HAL_RCC_GPIOC_CLK_ENABLE();
  __HAL_RCC_GPIOD_CLK_ENABLE();
  __HAL_RCC_GPIOA_CLK_ENABLE();
  __HAL_RCC_GPIOB_CLK_ENABLE();

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOB, GPIO_PIN_12|GPIO_PIN_13, GPIO_PIN_RESET);

  /*Configure GPIO pins : PA5 PA6 PA7 PA8 */
  GPIO_InitStruct.Pin = GPIO_PIN_5|GPIO_PIN_6|GPIO_PIN_7|GPIO_PIN_8;
  GPIO_InitStruct.Mode = GPIO_MODE_IT_RISING;
  GPIO_InitStruct.Pull = GPIO_PULLDOWN;
  HAL_GPIO_Init(GPIOA, &GPIO_InitStruct);

  /*Configure GPIO pin : PB12 */
  GPIO_InitStruct.Pin = GPIO_PIN_12;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_PULLDOWN;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOB, &GPIO_InitStruct);

  /*Configure GPIO pin : PB13 */
  GPIO_InitStruct.Pin = GPIO_PIN_13;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOB, &GPIO_InitStruct);

  /* EXTI interrupt init*/
  HAL_NVIC_SetPriority(EXTI9_5_IRQn, 1, 0);
  HAL_NVIC_EnableIRQ(EXTI9_5_IRQn);

/* USER CODE BEGIN MX_GPIO_Init_2 */
/* USER CODE END MX_GPIO_Init_2 */
}

/* USER CODE BEGIN 4 */

/* USER CODE END 4 */

/**
  * @brief  This function is executed in case of error occurrence.
  * @retval None
  */
void Error_Handler(void)
{
  /* USER CODE BEGIN Error_Handler_Debug */
  /* User can add his own implementation to report the HAL error return state */
  __disable_irq();
  while (1)
  {
  }
  /* USER CODE END Error_Handler_Debug */
}

#ifdef  USE_FULL_ASSERT
/**
  * @brief  Reports the name of the source file and the source line number
  *         where the assert_param error has occurred.
  * @param  file: pointer to the source file name
  * @param  line: assert_param error line source number
  * @retval None
  */
void assert_failed(uint8_t *file, uint32_t line)
{
  /* USER CODE BEGIN 6 */
  /* User can add his own implementation to report the file name and line number,
     ex: printf("Wrong parameters value: file %s on line %d\r\n", file, line) */
  /* USER CODE END 6 */
}
#endif /* USE_FULL_ASSERT */
