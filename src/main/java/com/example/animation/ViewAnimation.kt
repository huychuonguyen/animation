/**
 * ZOOO Animation
 *
 * Made by: huychuonguyen@gmail.com, at: 08/09/2020
 *
 * This class make the Swipe Animation for a View
 *
 * @param view the view that can be applied animation
 * @param animationType the type of animation (AnimationType.Rotate or AnimationType.LeftRight), its default is AnimationType.Rotate.
 * @constructor Creates an empty group.
 *
 */

package com.example.animation

import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.atan2

// set default animation is rotate
class ViewAnimation(var view: View, animationType: AnimationType = AnimationType.Rotate){

    enum class AnimationType(val typeName: String){
        LeftRight("LEFT-RIGHT"),
        Rotate("ROTATE"),
        Moving("MOVING")
    }

    enum class DragDirection(val direction: String){
        Left("LEFT"),
        Right("RIGHT")
    }

    var animationType: AnimationType = animationType
        set(value){
            field = value
            setViewAnimationDrag()
        }

    private var baseLeft = 0f
    private var baseRight = 0f
    private var dX = 0f
    private var distanceDrag = 0f

    var sensitivity = 1.5f

    var enableDragToLeft = true
    var enableDragToRight = true

    private var beginY = 0f
    private var lengthView = 0f
    private var angle = 0f

    private var isCanClick = true
    // detect when LongClicked is handled
    var isLongPress = false
    // LEFT: drag to left, RIGHT: drag to right
    var dragType: DragDirection = DragDirection.Left

    private var oldX = 0f

    private var d = 0f
    private var newRot = 0f


    companion object{
        private const val DISTANCE_DRAG = 330f
        private const val ACTION_ANGLE = 45f
        private const val COUNT_HANDLE = 8
        //this value to prevent other item can apply animation when current item is in animation
        var isOnAnimation = false
    }

    init {
        this.animationType = animationType
    }


    private fun setViewAnimationDrag(){
        when(animationType){
            AnimationType.Rotate -> setAnimationRotate()
            AnimationType.LeftRight -> setAnimationLeftRight()
            AnimationType.Moving -> setAnimationMoving()
        }
    }

    private fun setAnimationRotate(){
        this.view.setOnTouchListener(object: View.OnTouchListener{
            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                event!!
                when (event.actionMasked ) {

                    MotionEvent.ACTION_DOWN -> {
                        view!!
                        //save oldX
                        oldX = event.rawX
                        dX = (view.x - event.rawX)

                        if(!isOnAnimation)
                            iOnActionDown.onActionDown(view)

                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        view!!

                        if (!isLongPress && !isOnAnimation && abs(event.rawX - oldX) >= COUNT_HANDLE){

                            isOnAnimation = true
                            // detect is long press handle
                            beginY = event.y
                            isLongPress = true
                            isCanClick = false
                            // detect when finger drag to left or right
                            dragType = if(event.rawX > oldX)
                                DragDirection.Right
                            else
                                DragDirection.Left

                            lengthView = (view.right- view.left).toFloat() //view.width.toFloat()
                            d = getAngle(event)
                            //handle click
                            //onViewAnimationListener.onDragHandle(view)
                            iOnDragHandle.onDragHandle(view)
                        }
                        // if moving without long press
                        // we will don't drag view
                        if(!isLongPress)
                            return true

                        // check to prevent view drag down
                        newRot = getAngle(event)
                        if(newRot > d /*|| event.rawY > beginY */)
                            return true


                        if(dragType == DragDirection.Left)
                            rotateLeft(view, event)
                        else if(dragType == DragDirection.Right)
                            rotateRight(view, event)

                        return true
                    }
                    MotionEvent.ACTION_UP -> {

                        view!!
                        // handle onClick event
                        if( isCanClick && !isOnAnimation && abs(event.rawX - oldX) < COUNT_HANDLE)
                        {
                            iOnClicked.onClicked(view)
                            view.performClick()
                            return true
                        }


                        //Check angle to reverses to normal rotation
                        if(abs(view.rotation) < ACTION_ANGLE){

                            rotateTo(view,0f) {
                                iOnDragDismiss.onDragDismiss(view)
                                iOnViewReturnNormal.onReturnNormalEnd(view)

                                resetValue()
                                isOnAnimation = false
                            }
                        }
                        //Keep rotate to 180 angle
                        else {
                            if(dragType == DragDirection.Right/* && enableDragToRight*/)
                                rotateTo(view,180f) {
                                    iOnDragSuccess.onDragSuccess(view)
                                    resetValue()
                                }
                            else if (dragType == DragDirection.Left /*&& enableDragToLeft*/)
                                rotateTo(view,-180f) {
                                    iOnDragSuccess.onDragSuccess(view)
                                    resetValue()
                                }
                        }


                        // handle action up
                        iOnActionUp.onActionUp(view)

                        return true
                    }

                    MotionEvent.ACTION_CANCEL->{
                        view!!
                        // hanle action cancel
                        iOnActionCancel.onActionCancel(view)
                        resetValue()
                        //isOnAnimation = false
                        view.clearAnimation()

                        return true
                    }
                }
                return true
            }
        })
    }

    private fun setAnimationLeftRight(){
        this.view.setOnTouchListener(object: View.OnTouchListener{
            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                event!!
                when (event.actionMasked ) {

                    MotionEvent.ACTION_DOWN -> {
                        view!!
                        // get location of view on screen
                        baseLeft = view.left.toFloat() // getLocationOnScreen(view)[0].toFloat()  //view.left.toFloat() // viewLocation[0].toFloat()
                        baseRight = view.right.toFloat() // baseLeft +  view.width.toFloat()
                        dX = (view.x - event.rawX)*sensitivity
                        //save oldX
                        oldX = event.rawX

                        lengthView = view.right.toFloat() //baseLeft +  view.width.toFloat()

                        if(!isOnAnimation)
                            iOnActionDown.onActionDown(view)

                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        view!!

                        if (!isLongPress && !isOnAnimation && abs(event.rawX - oldX) >= COUNT_HANDLE){

                            isOnAnimation = true
                            // detect is long press handle
                            isLongPress = true
                            // if finger is moved out of COUNT_HANDLE, then can not handle clicked after finger up
                            isCanClick = false
                            // detect when finger drag to left or right
                            dragType = if(event.rawX > oldX)
                                DragDirection.Right
                            else
                                DragDirection.Left

                            lengthView = view.width.toFloat()
                            //handle click
                            iOnDragHandle.onDragHandle(view)
                        }
                        // if moving without long press
                        // we will don't drag view
                        if(!isLongPress)
                            return true

                        if(dragType == DragDirection.Left)
                            dragToLeft(view, event)
                        else if(dragType == DragDirection.Right)
                            dragToRight(view,event)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {

                        view!!
                        // handle onClick event
                        if(isCanClick && !isOnAnimation && abs(event.rawX - oldX) < COUNT_HANDLE){
                            iOnClicked.onClicked(view)
                            view.performClick()
                        }


                        //Check distance to reverses to normal rotation
                        if(abs(distanceDrag) < DISTANCE_DRAG){
                            dragToLocation(view, baseLeft){
                                iOnDragDismiss.onDragDismiss(view)
                                isOnAnimation = false
                            }
                        }

                        //Keep auto complete drag
                        else {
                            if(dragType == DragDirection.Left)
                                dragToLocation(view, -lengthView){iOnDragSuccess.onDragSuccess(view)}
                            else
                                dragToLocation(view, lengthView){iOnDragSuccess.onDragSuccess(view)}

                        }

                        // handle action up
                        iOnActionUp.onActionUp(view)

                        resetValue()

                        return true
                    }

                    MotionEvent.ACTION_CANCEL->{
                        view!!

                        // hanle action cancel
                        iOnActionCancel.onActionCancel(view)
                        resetValue()
                        isOnAnimation = false

                        return true
                    }
                }
                return true
            }
        })
    }

    // these variables bellow for moving animation
    private var oldXMoving = 0f
    private var oldYMoving = 0f
    private var baseViewX = 0
    private var baseViewY = 0
    private var baseViewZ = 0f

    //

    private fun setAnimationMoving(){
        this.view.setOnTouchListener(object: View.OnTouchListener{
            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                event?.let{ e ->
                    when (e.action ) {
                        MotionEvent.ACTION_DOWN -> {
                            view!!
                            if(!isOnAnimation){
                                baseViewX = view.left
                                baseViewY = view.top
                                baseViewZ = view.z
                                oldXMoving = e.rawX
                                oldYMoving = e.rawY
                                oldX = e.rawX

                                iOnActionDown.onActionDown(view)
                            }
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            view!!
                            val deltaX = abs(event.rawX - oldX)
                            if ( !isOnAnimation && (enableDragToLeft || enableDragToRight) && deltaX >= COUNT_HANDLE){

                                isOnAnimation = true
                                // if finger is moved out of COUNT_HANDLE, then can not handle clicked after finger up
                                isCanClick = false
                                // detect when finger drag to left or right
                                dragType = if(event.rawX > oldX)
                                    DragDirection.Right
                                else
                                    DragDirection.Left

                                view.z = baseViewZ + 1f
                                //handle click
                                iOnDragHandle.onDragHandle(view)
                            }

                            if((enableDragToLeft || enableDragToRight) && deltaX >= COUNT_HANDLE)
                                startMoving(view, event)

                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            view!!
                            // handle onClick event
                            if(isCanClick && !isOnAnimation && abs(event.rawX - oldX) < COUNT_HANDLE){
                                iOnClicked.onClicked(view)
                                view.performClick()
                            }

                            if(isOnAnimation && (enableDragToRight || enableDragToLeft)){
                                //Check distance to reverses to normal rotation
                                val deltaX = abs(event.rawX - oldXMoving)
                                val deltaY = abs(event.rawY - oldYMoving)
                                // handle dismiss listener
                                if(deltaX < DISTANCE_DRAG && deltaY < DISTANCE_DRAG){
                                    returnNormalMovingLocation(view){
                                        iOnDragDismiss.onDragDismiss(view)
                                        resetValue()
                                        isOnAnimation = false
                                    }
                                }
                                // handle success listener
                                else {
                                    returnNormalMovingLocation(view){
                                        iOnDragSuccess.onDragSuccess(view)
                                        resetValue()
                                    }
                                }
                            }
                            view.z = baseViewZ
                            // handle action up
                            iOnActionUp.onActionUp(view)

                            return true
                        }

                        MotionEvent.ACTION_CANCEL->{
                            view!!
                            view.z = baseViewZ
                            if(isOnAnimation && (enableDragToRight || enableDragToLeft)){
                                returnNormalMovingLocation(view){
                                    iOnDragSuccess.onDragSuccess(view)
                                    resetValue()
                                }
                            }
                            // handle action cancel
                            iOnActionCancel.onActionCancel(view)


                            return true
                        }
                    }
                    return true
                }
                return true
            }
        })
    }

    fun returnNormal(view: View, actionFunc: () -> Unit = {}){
        when(animationType){
            AnimationType.LeftRight -> returnNormalLocation(view){
                actionFunc()
            }
            AnimationType.Rotate -> returnNormalRotation(view){
                actionFunc()
            }
            AnimationType.Moving -> returnNormalMovingLocation(view){
                actionFunc()
            }
        }
        //isOnAnimation= false
    }

    private fun resetValue(){
        // reset value for moving animation
        oldXMoving = 0f
        oldYMoving = 0f
        // reset value for left-right animation
        oldX = 0f
        distanceDrag = 0f
        // reset value for rotate animation
        angle = 0f

        isLongPress = false
        isCanClick = true
        //isOnAnimation = false
    }

    private fun dragToLeft(view: View, event: MotionEvent) {
        val newX = event.rawX*sensitivity + dX
        // check to convert into drag right
        if( newX + view.width - COUNT_HANDLE > baseRight ){
            if(enableDragToRight){
                dragType = DragDirection.Right
                iOnDragHandle.onDragHandle(view)
            }
            return
        }
        // check to keep start drag from current rawX when drag from right -> left
        // (nothing happen because it be disabled by : enableDragToLeft = false )
        // then from this rawX point, we drag from left -> right, the view will be drag
        if(!enableDragToLeft){
            if(newX + COUNT_HANDLE < baseLeft ){
                oldX = event.rawX
                dX = (view.x - event.rawX)*sensitivity
            }
            return
        }

        distanceDrag = event.rawX - oldX
        view.x = newX
    }

    private fun dragToRight(view: View, event: MotionEvent) {
        val newX = event.rawX*sensitivity + dX
        // check to convert into drag left
        if(newX + COUNT_HANDLE < baseLeft ){
            if(enableDragToLeft){
                dragType = DragDirection.Left
                iOnDragHandle.onDragHandle(view)
            }
            return
        }

        if(!enableDragToRight) {
            if( newX + view.width - COUNT_HANDLE > baseRight ){
                oldX = event.rawX
                dX = (view.x - event.rawX)*sensitivity
            }
            return
        }

        distanceDrag = event.rawX - oldX
        view.x = newX
    }


    private fun startMoving(view: View, event: MotionEvent) {
        val dX = event.rawX - oldXMoving
        val dY = event.rawY - oldYMoving

        val newLocationX = baseViewX + dX
        val newLocationY = baseViewY + dY

        //val centerView = lengthView/2

        if(dX > 0 && enableDragToRight && dragType == DragDirection.Left){
            dragType = DragDirection.Right
            iOnDragHandle.onDragHandle(view)
        } else if (dX <= 0 && enableDragToLeft && dragType == DragDirection.Right) {
            dragType = DragDirection.Left
            iOnDragHandle.onDragHandle(view)
        }

        view.apply{
            x = newLocationX
            y = newLocationY
        }
    }
    private fun returnNormalRotation(view: View, actionFunc: () -> Unit = {}){
        rotateTo(view,0f) {
            android.os.Handler().postDelayed({
                actionFunc()
                iOnViewReturnNormal.onReturnNormalEnd(view)
                isOnAnimation = false
            }, 150L)

        }
    }

    private fun returnNormalLocation(view: View, actionFunc: () -> Unit = {}){
        dragToLocation(view, baseLeft) {
            actionFunc()
            iOnViewReturnNormal.onReturnNormalEnd(view)
            isOnAnimation = false
        }
    }

    private fun returnNormalMovingLocation(view: View, actionFunc: () -> Unit = {}){
        val baseX = baseViewX*1f
        val baseY = baseViewY*1f
        moveToLocation(view, baseX, baseY){
            actionFunc()
            isOnAnimation = false
            iOnViewReturnNormal.onReturnNormalEnd(view)

        }
    }


    private fun dragToLocation(view :View, locationX: Float, actionFunc: () -> Unit){
        view.animate().apply{
            x(locationX)
            duration = 200
            withEndAction {
                cancel()
                actionFunc()
            }
            start()
        }

    }

    private fun moveToLocation(view: View, locationX: Float, locationY: Float, action: () -> Unit = {}){
        view.animate().apply{
            x(locationX)
            y(locationY)
            duration = 300
            withEndAction {
                cancel()
                action()
            }
            start()
        }
    }

    private fun rotateLeft(view : View, event: MotionEvent){

        //newRot = getAngle(event)
        val r =   newRot - d

        angle = r*1.5f
        var currentAngle = angle
        // check to prevent when drag over 180 degrees
        if(currentAngle < -180f)
            currentAngle = -180f


        // check to convert into drag right
        if(newRot >= d && event.rawX >= oldX){
            if(enableDragToRight){
                dragType = DragDirection.Right
                iOnDragHandle.onDragHandle(view)
            }
        }


        // check to update value when user drag from left to right but it is unable
        // and then return to drag from right to left, so we will handle drag action from current point
        if(!enableDragToLeft){
            // isCanClickedHandle = true
            oldX = event.rawX
            d = getAngle(event)
            return
        }


        // rotate view
        view.let{
            //set pivot of view to rotate
            it.pivotX =  0f
            it.pivotY = (it.height/2).toFloat()
            //set angle to rotate when move finger
            //rotateView(it,0,currentAngle,currentAngle)
            it.rotation  = currentAngle
        }
    }
    private fun rotateRight(view: View, event: MotionEvent){

        //newRot = getAngle(event)
        val r =  newRot - d

        angle =  r*1.5f
        var currentAngle = -angle
        // check to prevent when drag over 180 degrees
        if(currentAngle > 180f)
            currentAngle = 180f

        // check to convert into drag left
        if(newRot >= d && event.rawX <= oldX){
            if(enableDragToLeft){
                dragType = DragDirection.Left
                iOnDragHandle.onDragHandle(view)
            }
        }


        // check to update value when user drag from left to right but it is unable
        // and then return to drag from right to left, so we will handle drag action from current point
        if(!enableDragToRight){
            //isCanClickedHandle = true
            oldX = event.rawX
            d = getAngle(event)
            return
        }


        // rotate view
        view.let{
            //set pivot of view to rotate
            it.pivotX =  it.width.toFloat()
            it.pivotY = (it.height/2).toFloat()
            //set angle to rotate when move finger
            //rotateView(it,0,currentAngle,currentAngle)
            it.rotation  = currentAngle
        }
    }

    private fun rotateTo(view :View, rotate: Float, actionFunc: () -> Unit){
        view.animate().apply {
            x(view.x)
            y(view.y)
            rotation(rotate)
            duration = 200
            withEndAction {
                cancel()
                actionFunc()
            }
            start()
        }
    }
    private fun getAngle(event: MotionEvent): Float {
        // notice:
        // get drag distance via ratio (lengthView) - Vuốt theo tỷ lệ khoảng cách -
        // it mean when user drag from every point in view, the view be set same rotate
        // at the end
        val delta =  abs(event.rawX - oldX)
        val ratio = if (dragType == DragDirection.Left) lengthView/oldX else lengthView/(lengthView-oldX)
        val currentX = lengthView - delta*ratio

        // when x decrease a "delta" value, y will increase a "delta" value
        val deltaY = -beginY - delta/2
        val deltaX = currentX

        val radians = atan2(deltaY, deltaX)

        val degrees =  (radians * (180 / Math.PI) ).toFloat()

        // these lines bellow to prevent view still rotating when finger return to oldX
        // and keep dragging throw oldX
        if(dragType == DragDirection.Left && event.rawX > oldX && abs(degrees) > 0)
            return d
        if(dragType != DragDirection.Left && event.rawX < oldX && abs(degrees) > 0)
            return d

        return degrees

    }


    private interface IOnDragDismiss{fun onDragDismiss(view: View){}}
    private interface IOnDragSuccess{fun onDragSuccess(view: View){}}
    private interface IOnDragHandle{fun onDragHandle(view: View){}}
    private interface IOnActionUp{fun onActionUp(view: View){}}
    private interface IOnActionDown{fun onActionDown(view: View){}}
    private interface IOnActionCancel{fun onActionCancel(view: View){}}
    interface IOnClicked{fun onClicked(view: View){}}
    private interface IOnViewReturnNormal{fun onReturnNormalEnd(view: View){}}

    private var iOnDragDismiss: IOnDragDismiss = object: IOnDragDismiss {}
    private var iOnDragSuccess: IOnDragSuccess = object: IOnDragSuccess {}
    private var iOnDragHandle: IOnDragHandle = object : IOnDragHandle {}
    private var iOnActionUp: IOnActionUp = object : IOnActionUp {}
    private var iOnActionDown: IOnActionDown = object : IOnActionDown {}
    private var iOnActionCancel: IOnActionCancel = object : IOnActionCancel {}
    private var iOnClicked: IOnClicked = object : IOnClicked {}
    private var iOnViewReturnNormal: IOnViewReturnNormal = object : IOnViewReturnNormal {}


    fun onDragDismiss(action: (View) -> Unit) : ViewAnimation {
        iOnDragDismiss =  object : IOnDragDismiss {
            override fun onDragDismiss(view: View) {
                super.onDragDismiss(view)
                action(view)
            }
        }
        return this
    }
    fun onDragSuccess(action: (View) -> Unit): ViewAnimation {
        iOnDragSuccess = object : IOnDragSuccess {
            override fun onDragSuccess(view: View) {
                super.onDragSuccess(view)
                action(view)
            }
        }
        return this
    }
    fun onDragHandle(action: (View) -> Unit): ViewAnimation {
        iOnDragHandle = object : IOnDragHandle {
            override fun onDragHandle(view: View) {
                super.onDragHandle(view)
                action(view)
            }
        }
        return this
    }
    fun onActionUp(action: (View) -> Unit): ViewAnimation {
        iOnActionUp = object : IOnActionUp {
            override fun onActionUp(view: View) {
                super.onActionUp(view)
                action(view)
            }
        }
        return this
    }
    fun onActionDown(action: (View) -> Unit): ViewAnimation {
        iOnActionDown = object : IOnActionDown {
            override fun onActionDown(view: View) {
                super.onActionDown(view)
                action(view)
            }
        }
        return this
    }
    fun onActionCancel(action: (View) -> Unit): ViewAnimation {
        iOnActionCancel = object : IOnActionCancel {
            override fun onActionCancel(view: View) {
                super.onActionCancel(view)
                action(view)
            }
        }
        return this
    }
    fun onClicked(action: (View) -> Unit): ViewAnimation {
        iOnClicked = object : IOnClicked {
            override fun onClicked(view: View) {
                super.onClicked(view)
                action(view)
            }
        }
        return this
    }
    fun onViewReturnNormal(action: (View) -> Unit): ViewAnimation {
        iOnViewReturnNormal = object : IOnViewReturnNormal {
            override fun onReturnNormalEnd(view: View) {
                super.onReturnNormalEnd(view)
                action(view)
            }
        }
        return this
    }

}

