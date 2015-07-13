fun main(args: Array<String>)
{
	if( more(5, 10) )
	{
		println("5 is more than 10")
	} 
	else 
	<selection>{
		println("5 isn't more than 10")
		println("Some meaningless println")
		println("And one more")
	}</selection>
	if( less(2, 3))		{
		println("2 is less than 3")
	} 
	else 
	{
		println("2 isn't less than 3")
		println("Some meaningless println")
		println("And one more")
	}
}
fun more(a: Int, b: Int)
	= a > b
fun less(a: Int, b: Int)
	= a < b